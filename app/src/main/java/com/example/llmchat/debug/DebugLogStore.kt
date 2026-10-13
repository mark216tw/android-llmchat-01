package com.example.llmchat.debug

import com.example.llmchat.data.datastore.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

enum class LogLevel { REQUEST, EVENT, RESPONSE, ERROR, INFO }

data class DebugLogEntry(
    val id: Long,
    val time: String,
    val level: LogLevel,
    val message: String
)

/**
 * In-memory, bounded debug recorder so network activity can be inspected
 * inside the app without Logcat. Safe to call from any thread (OkHttp
 * worker threads write here).
 */
@Singleton
class DebugLogStore @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var enabledFlag = true

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val sequence = AtomicLong(0L)
    private val _entries = MutableStateFlow<List<DebugLogEntry>>(emptyList())
    val entries: StateFlow<List<DebugLogEntry>> = _entries.asStateFlow()

    init {
        scope.launch {
            settingsDataStore.debugLogEnabled.collect { value ->
                enabledFlag = value
                _enabled.value = value
            }
        }
    }

    fun setEnabled(value: Boolean) {
        enabledFlag = value
        _enabled.value = value
        scope.launch { settingsDataStore.setDebugLogEnabled(value) }
    }

    fun log(level: LogLevel, message: String) {
        if (!enabledFlag) return
        val now = System.currentTimeMillis()
        val entry = DebugLogEntry(
            id = sequence.incrementAndGet(),
            time = formatTime(now),
            level = level,
            message = message.take(MAX_MESSAGE_LENGTH)
        )
        _entries.update { current ->
            val trimmed = if (current.size >= MAX_ENTRIES) {
                current.subList(current.size - MAX_ENTRIES + 1, current.size).toList()
            } else {
                current
            }
            trimmed + entry
        }
    }

    fun clear() {
        _entries.value = emptyList()
    }

    private fun formatTime(millis: Long): String =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(millis))

    private companion object {
        const val MAX_ENTRIES = 500
        const val MAX_MESSAGE_LENGTH = 4000
    }
}
