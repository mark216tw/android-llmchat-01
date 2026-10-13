package com.example.llmchat.domain.context

import com.example.llmchat.data.remote.dto.ChatMessageDto

/**
 * Assembles the message list that is actually sent to the model.
 *
 * The full history is always kept in the local database; this only trims the
 * subset that goes over the wire so the request stays within the token budget.
 * The system prompt is never dropped, and trimming always lands on a user
 * message boundary so the conversation does not start with an assistant turn.
 */
object ContextBuilder {

    const val ROLE_SYSTEM = "system"
    const val ROLE_USER = "user"
    const val ROLE_ASSISTANT = "assistant"

    data class Result(
        val messages: List<ChatMessageDto>,
        val omittedCount: Int,
        val estimatedTokens: Int
    )

    fun build(
        systemPrompt: String,
        history: List<ChatMessageDto>,
        newUserMessage: String,
        config: ContextConfig
    ): Result {
        val system = if (systemPrompt.isBlank()) {
            emptyList()
        } else {
            listOf(ChatMessageDto(ROLE_SYSTEM, systemPrompt))
        }

        val kept = ArrayDeque(history)
        kept.addLast(ChatMessageDto(ROLE_USER, newUserMessage))

        var omitted = 0
        while (kept.size > 1 &&
            TokenEstimator.estimateMessages(system + kept) > config.inputBudget
        ) {
            kept.removeFirst()
            omitted++
        }

        // Drop a leading assistant turn so the payload starts with a user turn.
        while (kept.isNotEmpty() && kept.first().role == ROLE_ASSISTANT) {
            kept.removeFirst()
            omitted++
        }

        val messages = system + kept
        return Result(
            messages = messages,
            omittedCount = omitted,
            estimatedTokens = TokenEstimator.estimateMessages(messages)
        )
    }
}
