package com.example.llmchat.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.llmchat.domain.model.Conversation

@Entity(
    tableName = "conversations",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val profileId: Long,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Conversation = Conversation(
        id = id,
        profileId = profileId,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
