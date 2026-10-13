package com.example.llmchat.data.remote

sealed interface StreamEvent {
    /** A chunk of the visible assistant answer. */
    data class Delta(val content: String) : StreamEvent

    /** A chunk of the model's "thinking" (DeepSeek reasoning_content). */
    data class Reasoning(val content: String) : StreamEvent

    /** The stream finished. [finishReason] is the last seen finish_reason. */
    data class Completed(val finishReason: String?) : StreamEvent

    data class Failed(val message: String) : StreamEvent
}
