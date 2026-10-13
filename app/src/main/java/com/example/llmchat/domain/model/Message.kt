package com.example.llmchat.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Message(
    val id: Long,
    val conversationId: Long,
    val role: ChatRole,
    val content: String,
    val createdAt: Long
)
