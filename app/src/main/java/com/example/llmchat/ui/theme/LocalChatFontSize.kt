package com.example.llmchat.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.util.Locale

/** Chat message / input font size, provided at the app root from user settings. */
val LocalChatFontSize = staticCompositionLocalOf<TextUnit> { 16.sp }

/** TTS locale, provided at the app root; null means "follow the system default". */
val LocalTtsLocale = staticCompositionLocalOf<Locale?> { null }
