package com.example.llmchat.domain.model

enum class ChatRole(val apiValue: String) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant");

    companion object {
        fun fromApiValue(value: String): ChatRole =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: ASSISTANT
    }
}
