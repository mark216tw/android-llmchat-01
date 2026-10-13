package com.example.llmchat.domain.context

/**
 * Controls how much of the stored history is sent to the model.
 *
 * @param contextWindow   total token budget advertised by the model
 * @param maxOutputTokens  tokens reserved for the model's reply
 * @param safetyMarginRatio fraction of the window kept as a safety buffer
 */
data class ContextConfig(
    val contextWindow: Int,
    val maxOutputTokens: Int,
    val safetyMarginRatio: Double = 0.05
) {
    val inputBudget: Int
        get() {
            val margin = (contextWindow * safetyMarginRatio).toInt()
            return (contextWindow - maxOutputTokens - margin).coerceAtLeast(MIN_BUDGET)
        }

    companion object {
        const val MIN_BUDGET = 256
    }
}
