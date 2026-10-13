package com.example.llmchat.ui.settings

import androidx.lifecycle.ViewModel
import com.example.llmchat.data.settings.AppSettingsStore
import com.example.llmchat.domain.model.ThemeMode
import com.example.llmchat.domain.model.TtsLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val store: AppSettingsStore
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = store.themeMode
    val chatFontSize: StateFlow<Float> = store.chatFontSize
    val alwaysRead: StateFlow<Boolean> = store.alwaysRead
    val ttsLanguage: StateFlow<TtsLanguage> = store.ttsLanguage

    fun setThemeMode(mode: ThemeMode) = store.setThemeMode(mode)

    fun setChatFontSize(size: Float) = store.setChatFontSize(size)

    fun setAlwaysRead(enabled: Boolean) = store.setAlwaysRead(enabled)

    fun setTtsLanguage(language: TtsLanguage) = store.setTtsLanguage(language)
}
