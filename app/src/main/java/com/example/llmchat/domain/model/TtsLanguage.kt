package com.example.llmchat.domain.model

/** Language used by text-to-speech. [SYSTEM] follows the device default. */
enum class TtsLanguage(val tag: String?) {
    SYSTEM(null),
    ZH("zh-TW"),
    EN("en-US");

    companion object {
        fun fromName(value: String?): TtsLanguage =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
