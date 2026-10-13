package com.example.llmchat.ui.chat

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Which assistant message is currently being read aloud (null = idle). */
data class SpeechState(
    val messageId: Long? = null,
    val paused: Boolean = false
)

/**
 * Thin wrapper around [TextToSpeech] that reads long replies chunk by chunk so
 * playback can be paused / resumed / stopped. Not thread-safe for external callers;
 * use it from the main thread (UI).
 */
class SpeechController(context: Context) {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ready = false

    private var chunks: List<String> = emptyList()
    private var index = 0
    private var paused = false
    private var session = 0L

    private var pendingMessageId: Long? = null
    private var pendingText: String? = null
    private var pendingLocale: Locale? = null

    private val _state = MutableStateFlow(SpeechState())
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts
                if (engine != null) {
                    engine.language = Locale.getDefault()
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit

                        override fun onDone(utteranceId: String?) {
                            val id = utteranceId ?: return
                            if (!id.startsWith("$session:")) return
                            if (paused) return
                            index++
                            if (index >= chunks.size) finish() else speakCurrent()
                        }

                        override fun onError(utteranceId: String?) {
                            val id = utteranceId ?: return
                            if (!id.startsWith("$session:")) return
                            finish()
                        }
                    })
                    ready = true
                    val text = pendingText
                    if (text != null) {
                        val id = pendingMessageId ?: -1L
                        val locale = pendingLocale
                        pendingText = null
                        pendingMessageId = null
                        pendingLocale = null
                        begin(id, text, locale)
                    }
                }
            } else {
                _errors.tryEmit("裝置不支援語音朗讀（TTS 初始化失敗）")
            }
        }
    }

    fun start(messageId: Long, text: String, locale: Locale? = null) {
        if (text.isBlank()) return
        if (!ready) {
            pendingMessageId = messageId
            pendingText = text
            pendingLocale = locale
            _state.value = SpeechState(messageId, paused = false)
            return
        }
        begin(messageId, text, locale)
    }

    fun togglePause() {
        val current = _state.value
        if (current.messageId == null) return
        if (current.paused) resume() else pause()
    }

    fun stop() {
        session++
        tts?.stop()
        finish()
    }

    fun shutdown() {
        session++
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        finish()
    }

    private fun begin(messageId: Long, text: String, locale: Locale?) {
        chunks = splitIntoChunks(text)
        index = 0
        paused = false
        session++
        if (chunks.isEmpty()) {
            finish()
            return
        }
        applyLanguage(locale)
        _state.value = SpeechState(messageId, paused = false)
        speakCurrent()
    }

    private fun applyLanguage(locale: Locale?) {
        val engine = tts ?: return
        val target = locale ?: Locale.getDefault()
        val result = engine.setLanguage(target)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            if (locale != null) {
                engine.setLanguage(Locale.getDefault())
                _errors.tryEmit("裝置不支援此朗讀語言，已改用系統預設語音")
            } else {
                _errors.tryEmit("裝置不支援此語言的語音資料")
            }
        }
        val voice = engine.voices?.firstOrNull { it.locale.language == target.language }
        if (voice != null) engine.voice = voice
    }

    private fun speakCurrent() {
        val engine = tts ?: return
        if (index >= chunks.size) {
            finish()
            return
        }
        engine.speak(chunks[index], TextToSpeech.QUEUE_FLUSH, null, "$session:$index")
    }

    private fun pause() {
        session++
        paused = true
        tts?.stop()
        _state.value = _state.value.copy(paused = true)
    }

    private fun resume() {
        if (_state.value.messageId == null || chunks.isEmpty()) return
        paused = false
        _state.value = _state.value.copy(paused = false)
        if (index >= chunks.size) finish() else speakCurrent()
    }

    private fun finish() {
        chunks = emptyList()
        index = 0
        paused = false
        _state.value = SpeechState()
    }

    private fun splitIntoChunks(text: String): List<String> {
        val normalized = text.replace("\r\n", "\n").trim()
        if (normalized.isEmpty()) return emptyList()
        val parts = normalized
            .split(Regex("(?<=[。！？!?；;\\n])"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val merged = mutableListOf<String>()
        val buffer = StringBuilder()
        for (part in parts) {
            if (buffer.isNotEmpty()) buffer.append(' ')
            buffer.append(part)
            if (buffer.length >= 40) {
                merged.add(buffer.toString())
                buffer.clear()
            }
        }
        if (buffer.isNotEmpty()) merged.add(buffer.toString())
        return merged
    }
}

/**
 * Light Markdown cleanup so the reply reads naturally via TTS.
 *
 * Removes heading (`#`), emphasis (`**`/`*`/`_`/`~~`), inline code backticks,
 * blockquote (`>`), unordered list bullets (`-`/`*`/`+`), code fences, table
 * separator rows and the `|` character, and horizontal rules. Links are reduced
 * to their label. Punctuation, digits, body text, ordered list numbers and
 * code-block contents (only the fences are stripped) are preserved. Table rows
 * are joined with `，`.
 */
fun markdownToPlainText(markdown: String): String {
    val lines = markdown.replace("\r\n", "\n").split("\n")
    val out = StringBuilder()
    var inCode = false
    for (raw in lines) {
        val line = raw.trim()
        if (line.startsWith("```")) {
            inCode = !inCode
            continue
        }
        if (inCode) {
            out.append(raw).append('\n')
            continue
        }
        if (line.isEmpty()) {
            out.append('\n')
            continue
        }
        // Table separator row, e.g. |---|:--:|
        if (line.contains('|') && line.replace(Regex("[|:\\-\\s]"), "").isEmpty()) {
            continue
        }
        // Horizontal rule
        if (line.matches(Regex("^([-*_])\\1{2,}$"))) {
            continue
        }
        // Table data row: join cells with '，'
        if (line.contains('|')) {
            val cells = line.trim('|').split('|').map { it.trim() }.filter { it.isNotEmpty() }
            out.append(cells.joinToString("，")).append('\n')
            continue
        }
        var s = line
        s = s.replace(Regex("^>+\\s?"), "")
        s = s.replace(Regex("^#{1,6}\\s+"), "")
        s = s.replace(Regex("^[-*+]\\s+"), "")
        s = stripInline(s)
        s = s.replace(Regex("[ \\t]+"), " ").trim()
        out.append(s).append('\n')
    }
    return out.toString().replace(Regex("\\n{3,}"), "\n\n").trim()
}

private fun stripInline(text: String): String {
    var s = text.replace(Regex("\\[([^\\]]+)\\]\\([^)]*\\)"), "$1")
    s = s.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
    s = s.replace(Regex("__(.+?)__"), "$1")
    s = s.replace(Regex("~~(.+?)~~"), "$1")
    s = s.replace(Regex("`([^`]*)`"), "$1")
    s = s.replace(Regex("(?<!\\*)\\*([^*\\n]+?)\\*(?!\\*)"), "$1")
    s = s.replace(Regex("(?<!_)_([^_\\n]+?)_(?!_)"), "$1")
    return s
}
