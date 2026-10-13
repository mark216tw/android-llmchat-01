package com.example.llmchat.data.repository

import com.example.llmchat.data.local.ConversationDao
import com.example.llmchat.data.local.ConversationEntity
import com.example.llmchat.data.local.MessageDao
import com.example.llmchat.data.local.MessageEntity
import com.example.llmchat.domain.model.ChatRole
import com.example.llmchat.domain.model.Conversation
import com.example.llmchat.domain.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getConversation(id: Long): Conversation? =
        conversationDao.getById(id)?.toDomain()

    suspend fun createConversation(profileId: Long, title: String): Long {
        val now = System.currentTimeMillis()
        return conversationDao.insert(
            ConversationEntity(
                profileId = profileId,
                title = title,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun deleteConversation(id: Long) = conversationDao.deleteById(id)

    suspend fun renameConversation(id: Long, title: String) =
        conversationDao.updateTitle(id, title)

    suspend fun touchConversation(id: Long) =
        conversationDao.touch(id, System.currentTimeMillis())

    fun observeMessages(conversationId: Long): Flow<List<Message>> =
        messageDao.observeByConversation(conversationId).map { list -> list.map { it.toDomain() } }

    suspend fun getMessages(conversationId: Long): List<Message> =
        messageDao.getByConversation(conversationId).map { it.toDomain() }

    suspend fun messageCount(conversationId: Long): Int =
        messageDao.countByConversation(conversationId)

    /**
     * Deletes a user question together with the assistant reply that follows
     * it (if any).
     */
    suspend fun deleteQuestionWithAnswer(conversationId: Long, userMessageId: Long) {
        val messages = messageDao.getByConversation(conversationId)
        val index = messages.indexOfFirst { it.id == userMessageId }
        if (index == -1) return
        messageDao.deleteById(messages[index].id)
        val next = messages.getOrNull(index + 1)
        if (next != null && next.role == ChatRole.ASSISTANT.apiValue) {
            messageDao.deleteById(next.id)
        }
        touchConversation(conversationId)
    }

    suspend fun addMessage(conversationId: Long, role: ChatRole, content: String): Long {
        val id = messageDao.insert(
            MessageEntity(
                conversationId = conversationId,
                role = role.apiValue,
                content = content,
                createdAt = System.currentTimeMillis()
            )
        )
        touchConversation(conversationId)
        return id
    }
}
