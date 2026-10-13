package com.example.llmchat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.llmchat.domain.model.Profile
import com.example.llmchat.domain.model.ReasoningEffort
import com.example.llmchat.domain.model.ThinkingMode

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val baseUrl: String,
    val model: String,
    val systemPrompt: String,
    val temperature: Double,
    val maxTokens: Int,
    val contextWindow: Int,
    val thinkingMode: String = ThinkingMode.DEFAULT.name,
    val reasoningEffort: String = ReasoningEffort.DEFAULT.name,
    val createdAt: Long
) {
    fun toDomain(apiKey: String): Profile = Profile(
        id = id,
        name = name,
        baseUrl = baseUrl,
        apiKey = apiKey,
        model = model,
        systemPrompt = systemPrompt,
        temperature = temperature,
        maxTokens = maxTokens,
        contextWindow = contextWindow,
        thinkingMode = ThinkingMode.fromName(thinkingMode),
        reasoningEffort = ReasoningEffort.fromName(reasoningEffort)
    )
}
