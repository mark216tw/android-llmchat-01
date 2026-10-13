package com.example.llmchat.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.llmchat.domain.model.ChatRole
import com.example.llmchat.domain.model.Message

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val conversationId: Long,
    val role: String,
    val content: String,
    val createdAt: Long
) {
    fun toDomain(): Message = Message(
        id = id,
        conversationId = conversationId,
        role = ChatRole.fromApiValue(role),
        content = content,
        createdAt = createdAt
    )
}
