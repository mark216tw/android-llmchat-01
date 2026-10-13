package com.example.llmchat.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    suspend fun getAll(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: Long): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: Long)
}
