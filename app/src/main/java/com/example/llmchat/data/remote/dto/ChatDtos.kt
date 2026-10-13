package com.example.llmchat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String
)

/**
 * DeepSeek thinking-mode configuration. Sent only when the user opts in, so
 * other OpenAI-compatible providers never receive an unknown field.
 */
@Serializable
data class ThinkingConfig(
    val type: String? = null,
    @SerialName("reasoning_effort") val reasoningEffort: String? = null
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean = false,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val thinking: ThinkingConfig? = null
)

@Serializable
data class ChatChoice(
    val index: Int = 0,
    val message: ChatMessageDto? = null,
    val delta: ChatMessageDto? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

/**
 * Streaming deltas are partial: a chunk may carry only `role`, only `content`,
 * both, or neither (the final `finish_reason` chunk). Every field is therefore
 * optional with a default so that decoding never throws on a partial chunk.
 */
@Serializable
data class ChatDeltaDto(
    val role: String? = null,
    val content: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null
)

@Serializable
data class StreamChoice(
    val index: Int = 0,
    val delta: ChatDeltaDto? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class ChatResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<ChatChoice> = emptyList()
)

@Serializable
data class ModelInfo(
    val id: String = ""
)

@Serializable
data class ModelsResponse(
    val data: List<ModelInfo> = emptyList()
)
