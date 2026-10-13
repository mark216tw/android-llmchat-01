package com.example.llmchat.data.settings

import com.example.llmchat.data.datastore.SettingsDataStore
import com.example.llmchat.domain.model.ThemeMode
import com.example.llmchat.domain.model.TtsLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide UI settings (theme mode, chat font size) backed by DataStore.
 * Holds hot [StateFlow]s so both the app shell and the settings screen
 * observe the same live values.
 */
@Singleton
class AppSettingsStore @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _chatFontSize = MutableStateFlow(SettingsDataStore.DEFAULT_CHAT_FONT_SIZE)
    val chatFontSize: StateFlow<Float> = _chatFontSize.asStateFlow()

    private val _alwaysRead = MutableStateFlow(false)
    val alwaysRead: StateFlow<Boolean> = _alwaysRead.asStateFlow()

    private val _ttsLanguage = MutableStateFlow(TtsLanguage.SYSTEM)
    val ttsLanguage: StateFlow<TtsLanguage> = _ttsLanguage.asStateFlow()

    init {
        scope.launch {
            settingsDataStore.themeMode.collect { _themeMode.value = it }
        }
        scope.launch {
            settingsDataStore.chatFontSize.collect { _chatFontSize.value = it }
        }
        scope.launch {
            settingsDataStore.alwaysRead.collect { _alwaysRead.value = it }
        }
        scope.launch {
            settingsDataStore.ttsLanguage.collect { _ttsLanguage.value = it }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        scope.launch { settingsDataStore.setThemeMode(mode) }
    }

    fun setChatFontSize(size: Float) {
        _chatFontSize.value = size
        scope.launch { settingsDataStore.setChatFontSize(size) }
    }

    fun setAlwaysRead(enabled: Boolean) {
        _alwaysRead.value = enabled
        scope.launch { settingsDataStore.setAlwaysRead(enabled) }
    }

    fun setTtsLanguage(language: TtsLanguage) {
        _ttsLanguage.value = language
        scope.launch { settingsDataStore.setTtsLanguage(language) }
    }
}
