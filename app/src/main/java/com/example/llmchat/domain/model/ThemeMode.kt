package com.example.llmchat.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromName(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
