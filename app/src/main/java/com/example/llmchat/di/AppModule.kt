package com.example.llmchat.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.llmchat.data.local.AppDatabase
import com.example.llmchat.data.local.ConversationDao
import com.example.llmchat.data.local.MessageDao
import com.example.llmchat.data.local.ProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        // A finite idle timeout: long generations are fine as long as tokens
        // keep arriving, but a truly stuck connection will eventually fail
        // instead of hanging forever.
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE profiles ADD COLUMN thinkingMode TEXT NOT NULL DEFAULT 'DEFAULT'"
            )
            db.execSQL(
                "ALTER TABLE profiles ADD COLUMN reasoningEffort TEXT NOT NULL DEFAULT 'DEFAULT'"
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "llmchat.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideConversationDao(db: AppDatabase): ConversationDao = db.conversationDao()

    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()
}
