package com.example.llmchat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: Long): ConversationEntity?

    @Insert
    suspend fun insert(conversation: ConversationEntity): Long

    @Query("UPDATE conversations SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)

    @Query("UPDATE conversations SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: Long, updatedAt: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
