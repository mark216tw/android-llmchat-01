package com.example.llmchat.data.remote

import com.example.llmchat.data.remote.dto.ChatRequest
import com.example.llmchat.data.remote.dto.StreamChoice
import com.example.llmchat.debug.DebugLogStore
import com.example.llmchat.debug.LogLevel
import com.example.llmchat.util.AppLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Streams a chat completion using the OpenAI-compatible Server-Sent Events
 * protocol (`POST {baseUrl}/chat/completions` with `stream = true`).
 *
 * The stream is considered finished when either `data: [DONE]` is received or
 * a chunk carries a non-null `finish_reason` (some providers only send the
 * latter). DeepSeek's `reasoning_content` is surfaced as [StreamEvent.Reasoning]
 * so thinking models do not look like they returned nothing.
 */
@Singleton
class ChatStreamClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val debugLogStore: DebugLogStore
) {
    fun streamChat(
        baseUrl: String,
        apiKey: String,
        request: ChatRequest
    ): Flow<StreamEvent> = callbackFlow {
        val url = baseUrl.trimEnd('/') + "/chat/completions"
        val payload = json.encodeToString(
            ChatRequest.serializer(),
            request.copy(stream = true)
        )
        AppLog.d("POST $url")
        AppLog.d("body: $payload")
        debugLogStore.log(LogLevel.REQUEST, "POST $url\n$payload")

        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        var finishReason: String? = null
        var closed = false

        fun finish() {
            if (closed) return
            closed = true
            trySend(StreamEvent.Completed(finishReason))
            close()
        }

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                debugLogStore.log(LogLevel.EVENT, data)

                if (data == "[DONE]") {
                    finish()
                    return
                }

                val choice = runCatching {
                    json.decodeFromString(StreamChoice.serializer(), data)
                }.getOrNull()

                if (choice == null) {
                    AppLog.d("unparsed event: $data")
                    debugLogStore.log(LogLevel.ERROR, "unparsed event: $data")
                    return
                }

                choice.delta?.let { delta ->
                    if (!delta.reasoningContent.isNullOrEmpty()) {
                        trySend(StreamEvent.Reasoning(delta.reasoningContent))
                    }
                    if (!delta.content.isNullOrEmpty()) {
                        trySend(StreamEvent.Delta(delta.content))
                    }
                }

                if (choice.finishReason != null) {
                    finishReason = choice.finishReason
                    finish()
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                val statusMessage = response?.let { "HTTP ${it.code}" }
                    ?: (t?.message ?: "Unknown streaming error")
                // okhttp-sse does not consume the body of an unsuccessful
                // response before calling onFailure, so the server's error
                // payload is still readable here.
                val errorBody = runCatching { response?.body?.string() }.getOrNull()
                val detail = if (errorBody.isNullOrBlank()) {
                    statusMessage
                } else {
                    "$statusMessage\n$errorBody"
                }
                AppLog.d("onFailure: $detail")
                debugLogStore.log(LogLevel.ERROR, "onFailure: $detail")
                if (!closed) {
                    closed = true
                    val failedMessage = if (errorBody.isNullOrBlank()) {
                        statusMessage
                    } else {
                        "$statusMessage：${errorBody.take(200)}"
                    }
                    trySend(StreamEvent.Failed(failedMessage))
                    close()
                }
            }

            override fun onClosed(eventSource: EventSource) {
                finish()
            }
        }

        val eventSource = EventSources.createFactory(client)
            .newEventSource(httpRequest, listener)

        awaitClose { eventSource.cancel() }
    }
}
