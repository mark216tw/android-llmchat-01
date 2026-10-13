package com.example.llmchat.data.remote

import com.example.llmchat.data.remote.dto.ChatRequest
import com.example.llmchat.data.remote.dto.ChatResponse
import com.example.llmchat.data.remote.dto.ModelsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAiApiService {

    @GET("models")
    suspend fun listModels(
        @Header("Authorization") authorization: String
    ): ModelsResponse

    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body body: ChatRequest
    ): ChatResponse
}
