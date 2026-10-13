package com.example.llmchat.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Conversation(
    val id: Long,
    val profileId: Long,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)
