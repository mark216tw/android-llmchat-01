package com.example.llmchat.domain.context

import com.example.llmchat.data.remote.dto.ChatMessageDto

/**
 * Zero-dependency heuristic token estimator.
 *
 * A widely used rule of thumb is that English text averages roughly four
 * characters per token, while CJK characters are usually one token each.
 * This is intentionally an approximation: it is fast, has no dependencies,
 * and only needs to be accurate enough to stay under a model's context window.
 */
object TokenEstimator {

    private const val MESSAGE_OVERHEAD = 4

    fun estimate(text: String): Int {
        if (text.isEmpty()) return 0
        var cjk = 0
        var other = 0
        for (ch in text) {
            if (isCjk(ch)) cjk++ else other++
        }
        // Round up the non-CJK bucket so short strings still cost at least 1.
        return cjk + (other + 3) / 4
    }

    fun estimateMessage(message: ChatMessageDto): Int =
        estimate(message.role) + estimate(message.content) + MESSAGE_OVERHEAD

    fun estimateMessages(messages: List<ChatMessageDto>): Int =
        messages.sumOf { estimateMessage(it) }

    private fun isCjk(ch: Char): Boolean {
        val code = ch.code
        return code in 0x3040..0x30FF ||   // Japanese kana
            code in 0x3400..0x4DBF ||      // CJK Extension A
            code in 0x4E00..0x9FFF ||      // CJK Unified Ideographs
            code in 0xF900..0xFAFF ||      // CJK Compatibility Ideographs
            code in 0xAC00..0xD7AF ||      // Hangul syllables
            code in 0xFF00..0xFFEF         // Full-width forms
    }
}
