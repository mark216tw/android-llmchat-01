package com.example.llmchat.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds (and caches) an [OpenAiApiService] for an arbitrary user-supplied base
 * URL, since each profile can point at a different host. Caching avoids
 * rebuilding Retrofit on every connection test / non-streaming fallback.
 */
@Singleton
class OpenAiClientFactory @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json
) {
    private val cache = ConcurrentHashMap<String, OpenAiApiService>()

    fun create(baseUrl: String): OpenAiApiService {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return cache.computeIfAbsent(normalized) {
            Retrofit.Builder()
                .baseUrl(it)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(OpenAiApiService::class.java)
        }
    }
}
