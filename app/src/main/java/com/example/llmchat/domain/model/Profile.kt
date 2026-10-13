package com.example.llmchat.domain.model

import androidx.compose.runtime.Immutable

/**
 * Controls whether the provider runs in "thinking" mode. [DEFAULT] leaves the
 * decision to the server (the `thinking` field is not sent at all).
 */
enum class ThinkingMode {
    DEFAULT,
    ENABLED,
    DISABLED;

    companion object {
        fun fromName(value: String?): ThinkingMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
    }
}

/**
 * Controls the DeepSeek `reasoning_effort`. [DEFAULT] omits the field.
 */
enum class ReasoningEffort(val apiValue: String?) {
    DEFAULT(null),
    NONE("none"),
    LOW("low"),
    HIGH("high"),
    MAX("max");

    companion object {
        fun fromName(value: String?): ReasoningEffort =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
    }
}

@Immutable
data class Profile(
    val id: Long = 0L,
    val name: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val systemPrompt: String = "",
    val temperature: Double = 0.7,
    /** 0 means "do not send max_tokens", letting the server use its own default. */
    val maxTokens: Int = 0,
    val contextWindow: Int = 8192,
    val thinkingMode: ThinkingMode = ThinkingMode.DEFAULT,
    val reasoningEffort: ReasoningEffort = ReasoningEffort.DEFAULT
)
