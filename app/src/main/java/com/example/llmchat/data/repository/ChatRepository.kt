package com.example.llmchat.data.repository

import com.example.llmchat.data.remote.ChatStreamClient
import com.example.llmchat.data.remote.OpenAiClientFactory
import com.example.llmchat.data.remote.StreamEvent
import com.example.llmchat.data.remote.dto.ChatMessageDto
import com.example.llmchat.data.remote.dto.ChatRequest
import com.example.llmchat.data.remote.dto.ThinkingConfig
import com.example.llmchat.debug.DebugLogStore
import com.example.llmchat.debug.LogLevel
import com.example.llmchat.domain.model.Profile
import com.example.llmchat.domain.model.ReasoningEffort
import com.example.llmchat.domain.model.ThinkingMode
import com.example.llmchat.util.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class ChatRepository @Inject constructor(
    private val clientFactory: OpenAiClientFactory,
    private val streamClient: ChatStreamClient,
    private val debugLogStore: DebugLogStore
) {
    /**
     * Verifies the endpoint by calling `GET /models` and returns the list of
     * model ids the server reports. This is also used to populate the model
     * picker so the user does not have to type an id by hand.
     */
    suspend fun fetchModels(baseUrl: String, apiKey: String): Result<List<String>> {
        return try {
            val response = retryTransient {
                clientFactory.create(baseUrl).listModels("Bearer $apiKey")
            }
            val models = response.data
                .map { it.id }
                .filter { it.isNotBlank() }
                .sorted()
            Result.success(models)
        } catch (e: HttpException) {
            val message = describeHttpError(e)
            debugLogStore.log(LogLevel.ERROR, "GET /models failed: $message")
            Result.failure(IOException(message))
        } catch (e: Exception) {
            debugLogStore.log(LogLevel.ERROR, "GET /models failed: ${e.message}")
            Result.failure(IOException(e.message ?: "連線失敗"))
        }
    }

    fun stream(
        profile: Profile,
        messages: List<ChatMessageDto>
    ): Flow<StreamEvent> {
        val request = ChatRequest(
            model = profile.model,
            messages = messages,
            stream = true,
            temperature = profile.temperature.roundTo2(),
            // 0 = do not send max_tokens: let the server use its own default
            // (important for thinking models, which count reasoning tokens).
            maxTokens = profile.maxTokens.takeIf { it > 0 },
            thinking = profile.thinkingConfig()
        )

        return flow {
            var receivedContent = false
            var failed = false
            var finishReason: String? = null

            // Retry only the connection while nothing has been received yet; once
            // any content is streamed we stop retrying to avoid duplicating output.
            var attempt = 0
            while (true) {
                var failedEvent: StreamEvent.Failed? = null
                streamClient.streamChat(
                    baseUrl = profile.baseUrl,
                    apiKey = profile.apiKey,
                    request = request
                ).collect { event ->
                    when (event) {
                        is StreamEvent.Delta -> {
                            receivedContent = true
                            emit(event)
                        }

                        is StreamEvent.Reasoning -> {
                            // Reasoning deliberately counts as "received content": a
                            // thinking model that only emitted reasoning did respond,
                            // so we do not retry with a non-streaming request.
                            receivedContent = true
                            emit(event)
                        }

                        is StreamEvent.Completed -> {
                            finishReason = event.finishReason
                        }

                        is StreamEvent.Failed -> {
                            failedEvent = event
                        }
                    }
                }

                if (failedEvent != null && !receivedContent && attempt < MAX_STREAM_ATTEMPTS - 1) {
                    attempt++
                    val backoff = STREAM_RETRY_BASE_DELAY_MS * attempt
                    debugLogStore.log(
                        LogLevel.INFO,
                        "stream failed, retry $attempt/$MAX_STREAM_ATTEMPTS in ${backoff}ms"
                    )
                    delay(backoff)
                    continue
                }
                if (failedEvent != null) {
                    failed = true
                    emit(failedEvent)
                }
                break
            }

            // Some servers ignore `stream = true` and answer with a single JSON
            // body instead of SSE. If literally nothing streamed and nothing
            // failed, fall back to a regular non-streaming request.
            if (!receivedContent && !failed) {
                debugLogStore.log(LogLevel.INFO, "no stream content, trying non-streaming fallback")
                val result = runCatching {
                    retryTransient {
                        clientFactory.create(profile.baseUrl).chatCompletion(
                            authorization = "Bearer ${profile.apiKey}",
                            body = request.copy(stream = false)
                        )
                    }
                }
                result.fold(
                    onSuccess = { response ->
                        val choice = response.choices.firstOrNull()
                        finishReason = choice?.finishReason ?: finishReason
                        val text = choice?.message?.content.orEmpty()
                        debugLogStore.log(
                            LogLevel.RESPONSE,
                            "fallback finish_reason=$finishReason, contentLength=${text.length}"
                        )
                        if (text.isNotEmpty()) {
                            emit(StreamEvent.Delta(text))
                        }
                    },
                    onFailure = { error ->
                        failed = true
                        debugLogStore.log(LogLevel.ERROR, "fallback failed: ${error.message}")
                        emit(StreamEvent.Failed(error.message ?: "非串流請求失敗"))
                    }
                )
            }

            AppLog.d("stream completed, finish_reason=$finishReason")
            debugLogStore.log(
                LogLevel.RESPONSE,
                "stream completed, finish_reason=$finishReason, receivedContent=$receivedContent"
            )
            emit(StreamEvent.Completed(finishReason))
        }
    }

    /** Retries [block] for transient network/server errors with exponential backoff. */
    private suspend fun <T> retryTransient(
        maxAttempts: Int = MAX_ATTEMPTS,
        initialDelayMs: Long = RETRY_BASE_DELAY_MS,
        block: suspend () -> T
    ): T {
        var attempt = 0
        var delayMs = initialDelayMs
        while (true) {
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isTransient(e) || ++attempt >= maxAttempts) throw e
                delay(delayMs)
                delayMs *= 2
            }
        }
    }

    private fun isTransient(e: Exception): Boolean = when (e) {
        is IOException -> true
        is HttpException -> e.code() == 429 || e.code() in 500..599
        else -> false
    }

    private fun Profile.thinkingConfig(): ThinkingConfig? {
        val type = when (thinkingMode) {
            ThinkingMode.ENABLED -> "enabled"
            ThinkingMode.DISABLED -> "disabled"
            ThinkingMode.DEFAULT ->
                if (reasoningEffort != ReasoningEffort.DEFAULT) "enabled" else null
        }
        val effort = reasoningEffort.apiValue
        if (type == null && effort == null) return null
        return ThinkingConfig(type = type, reasoningEffort = effort)
    }

    private fun Double.roundTo2(): Double = (this * 100).roundToInt() / 100.0

    private fun describeHttpError(e: HttpException): String {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val hint = when (e.code()) {
            401 -> "API Key 無效或未授權"
            403 -> "存取被拒"
            404 -> "找不到端點，請確認 Base URL 是否包含 /v1"
            429 -> "請求過於頻繁（額度限制）"
            in 500..599 -> "伺服器錯誤"
            else -> "HTTP ${e.code()}"
        }
        return if (body.isNullOrBlank()) hint else "$hint：$body"
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val RETRY_BASE_DELAY_MS = 500L
        const val MAX_STREAM_ATTEMPTS = 3
        const val STREAM_RETRY_BASE_DELAY_MS = 600L
    }
}
