package com.example.llmchat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY id ASC")
    fun observeByConversation(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY id ASC")
    suspend fun getByConversation(conversationId: Long): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun countByConversation(conversationId: Long): Int

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: Long)
}
