package com.example.llmchat.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.llmchat.domain.model.ThemeMode
import com.example.llmchat.domain.model.TtsLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val currentProfileId: Flow<Long> =
        context.settingsDataStore.data.map { it[KEY_CURRENT_PROFILE] ?: NO_PROFILE }

    val debugLogEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_DEBUG_LOG] ?: true }

    val themeMode: Flow<ThemeMode> =
        context.settingsDataStore.data.map { ThemeMode.fromName(it[KEY_THEME_MODE]) }

    val chatFontSize: Flow<Float> =
        context.settingsDataStore.data.map { it[KEY_CHAT_FONT_SIZE] ?: DEFAULT_CHAT_FONT_SIZE }

    val alwaysRead: Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_ALWAYS_READ] ?: false }

    val ttsLanguage: Flow<TtsLanguage> =
        context.settingsDataStore.data.map { TtsLanguage.fromName(it[KEY_TTS_LANGUAGE]) }

    suspend fun setCurrentProfileId(id: Long) {
        context.settingsDataStore.edit { it[KEY_CURRENT_PROFILE] = id }
    }

    suspend fun setDebugLogEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_DEBUG_LOG] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setChatFontSize(size: Float) {
        context.settingsDataStore.edit { it[KEY_CHAT_FONT_SIZE] = size }
    }

    suspend fun setAlwaysRead(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_ALWAYS_READ] = enabled }
    }

    suspend fun setTtsLanguage(language: TtsLanguage) {
        context.settingsDataStore.edit { it[KEY_TTS_LANGUAGE] = language.name }
    }

    companion object {
        const val NO_PROFILE = -1L
        const val DEFAULT_CHAT_FONT_SIZE = 16f
        private val KEY_CURRENT_PROFILE = longPreferencesKey("current_profile_id")
        private val KEY_DEBUG_LOG = booleanPreferencesKey("debug_log_enabled")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_CHAT_FONT_SIZE = floatPreferencesKey("chat_font_size")
        private val KEY_ALWAYS_READ = booleanPreferencesKey("always_read")
        private val KEY_TTS_LANGUAGE = stringPreferencesKey("tts_language")
    }
}
