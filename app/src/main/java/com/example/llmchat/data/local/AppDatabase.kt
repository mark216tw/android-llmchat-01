package com.example.llmchat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ProfileEntity::class, ConversationEntity::class, MessageEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
