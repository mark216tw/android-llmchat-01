package com.example.llmchat.ui.chat

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.llmchat.data.remote.StreamEvent
import com.example.llmchat.data.remote.dto.ChatMessageDto
import com.example.llmchat.data.repository.ChatRepository
import com.example.llmchat.data.repository.ConversationRepository
import com.example.llmchat.data.repository.ProfileRepository
import com.example.llmchat.data.settings.AppSettingsStore
import com.example.llmchat.domain.context.ContextBuilder
import com.example.llmchat.domain.context.ContextConfig
import com.example.llmchat.domain.model.ChatRole
import com.example.llmchat.domain.model.Conversation
import com.example.llmchat.domain.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Request to (auto-)read an assistant reply aloud. */
data class AutoReadEvent(val messageId: Long, val text: String)

@Immutable
data class ChatUiState(
    val conversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val streamingText: String = "",
    val reasoningText: String = "",
    val isStreaming: Boolean = false,
    val error: String? = null,
    val omittedCount: Int = 0,
    val estimatedTokens: Int = 0,
    val inputText: String = "",
    val isLoadingMessages: Boolean = true
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val conversationRepository: ConversationRepository,
    private val chatRepository: ChatRepository,
    private val appSettingsStore: AppSettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _autoReadEvents = MutableSharedFlow<AutoReadEvent>(extraBufferCapacity = 1)
    val autoReadEvents: SharedFlow<AutoReadEvent> = _autoReadEvents.asSharedFlow()

    private var conversationId: Long = -1L
    private var messagesJob: Job? = null
    private var sendJob: Job? = null

    fun init(id: Long) {
        if (id <= 0L) {
            // Draft conversation: nothing is persisted until the first message is sent.
            _uiState.update { it.copy(isLoadingMessages = false) }
            return
        }
        if (conversationId != id) {
            conversationId = id
            _uiState.update { it.copy(isLoadingMessages = true) }
            viewModelScope.launch {
                // Load the conversation and its messages in a single pass so the
                // title and content appear together, avoiding a two-step flash
                // (title first, then messages after a spinner).
                val conversation = conversationRepository.getConversation(id)
                val messages = conversationRepository.getMessages(id)
                _uiState.update {
                    it.copy(
                        conversation = conversation,
                        messages = messages,
                        isLoadingMessages = false
                    )
                }
            }
        }
        startObservingMessages(id)
    }

    private fun startObservingMessages(id: Long) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            conversationRepository.observeMessages(id).collect { messages ->
                _uiState.update { it.copy(messages = messages, isLoadingMessages = false) }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /** Loads a question's content into the input box so the user can ask it again. */
    fun reask(message: Message) {
        if (message.role != ChatRole.USER) return
        if (_uiState.value.isStreaming) return
        _uiState.update { it.copy(inputText = message.content, error = null) }
    }

    /** Deletes a user question together with its assistant reply. */
    fun deleteQuestion(message: Message) {
        if (message.role != ChatRole.USER) return
        val convId = _uiState.value.conversation?.id ?: conversationId
        if (convId <= 0L) return
        viewModelScope.launch {
            conversationRepository.deleteQuestionWithAnswer(convId, message.id)
        }
    }

    fun send() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        // Never let a previous (possibly stalled) request block a new one.
        sendJob?.cancel()

        sendJob = viewModelScope.launch {
            val existing = _uiState.value.conversation
                ?: if (conversationId > 0L) {
                    conversationRepository.getConversation(conversationId)
                } else {
                    null
                }

            val profile = existing?.let { profileRepository.getProfile(it.profileId) }
                ?: profileRepository.getCurrentProfile()
            if (profile == null) {
                _uiState.update { it.copy(error = "找不到可用的 API 設定") }
                return@launch
            }
            if (profile.baseUrl.isBlank() || profile.model.isBlank()) {
                _uiState.update { it.copy(error = "此 API 設定缺少 Base URL 或模型名稱") }
                return@launch
            }

            val convId: Long
            if (existing == null) {
                // Draft: only persist the conversation now that the user actually sent something.
                convId = conversationRepository.createConversation(profile.id, titleFrom(text))
                conversationId = convId
                startObservingMessages(convId)
                _uiState.update {
                    it.copy(conversation = conversationRepository.getConversation(convId))
                }
            } else {
                convId = existing.id
            }

            val history = conversationRepository.getMessages(convId)
                .map { ChatMessageDto(it.role.apiValue, it.content) }
            // Reserve output space even when max_tokens is not sent, so history
            // trimming still leaves room for the reply.
            val reservedOutput = if (profile.maxTokens > 0) profile.maxTokens else DEFAULT_OUTPUT_RESERVE
            val config = ContextConfig(
                contextWindow = profile.contextWindow,
                maxOutputTokens = reservedOutput
            )
            val built = ContextBuilder.build(
                systemPrompt = profile.systemPrompt,
                history = history,
                newUserMessage = text,
                config = config
            )

            _uiState.update {
                it.copy(
                    inputText = "",
                    error = null,
                    isStreaming = true,
                    streamingText = "",
                    reasoningText = "",
                    omittedCount = built.omittedCount,
                    estimatedTokens = built.estimatedTokens
                )
            }

            val wasEmpty = conversationRepository.messageCount(convId) == 0
            conversationRepository.addMessage(convId, ChatRole.USER, text)
            if (wasEmpty) {
                conversationRepository.renameConversation(convId, titleFrom(text))
            }

            val answer = StringBuilder()
            val reasoning = StringBuilder()
            var finishReason: String? = null
            var failed = false
            var cancelled = false
            var lastUiUpdate = 0L

            fun flushStreamingUi() {
                _uiState.update {
                    it.copy(streamingText = answer.toString(), reasoningText = reasoning.toString())
                }
            }

            try {
                chatRepository.stream(profile, built.messages).collect { event ->
                    when (event) {
                        is StreamEvent.Delta -> {
                            answer.append(event.content)
                            val now = System.currentTimeMillis()
                            if (now - lastUiUpdate >= STREAM_UI_INTERVAL_MS) {
                                lastUiUpdate = now
                                flushStreamingUi()
                            }
                        }

                        is StreamEvent.Reasoning -> {
                            reasoning.append(event.content)
                            val now = System.currentTimeMillis()
                            if (now - lastUiUpdate >= STREAM_UI_INTERVAL_MS) {
                                lastUiUpdate = now
                                flushStreamingUi()
                            }
                        }

                        is StreamEvent.Completed -> {
                            finishReason = event.finishReason
                            flushStreamingUi()
                        }

                        is StreamEvent.Failed -> {
                            failed = true
                            _uiState.update { it.copy(error = event.message) }
                        }
                    }
                }
            } catch (e: CancellationException) {
                cancelled = true
                throw e
            } catch (e: Exception) {
                failed = true
                _uiState.update { it.copy(error = e.message ?: "發生未知錯誤") }
            } finally {
                withContext(NonCancellable) {
                    if (answer.isNotEmpty()) {
                        val reply = answer.toString()
                        val replyId = conversationRepository.addMessage(convId, ChatRole.ASSISTANT, reply)
                        if (appSettingsStore.alwaysRead.value) {
                            _autoReadEvents.tryEmit(AutoReadEvent(replyId, reply))
                        }
                    } else if (!failed && !cancelled && _uiState.value.error == null) {
                        _uiState.update {
                            it.copy(error = emptyReplyMessage(reasoning.isNotEmpty(), finishReason))
                        }
                    }
                    _uiState.update {
                        it.copy(isStreaming = false, streamingText = "", reasoningText = "")
                    }
                }
            }
        }
    }

    fun stop() {
        sendJob?.cancel()
        sendJob = null
        _uiState.update { it.copy(isStreaming = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun titleFrom(text: String): String =
        text.replace('\n', ' ').take(20).ifBlank { "新對話" }

    private fun emptyReplyMessage(hadReasoning: Boolean, finishReason: String?): String {
        val reason = finishReason?.let { "（finish_reason=$it）" } ?: ""
        return when (finishReason) {
            "length" ->
                "已達輸出長度上限，未產生完整回覆$reason。請提高 Max Output Tokens 或縮短問題。"

            "content_filter" ->
                "內容被安全過濾機制阻擋，未產生回覆$reason。"

            "tool_calls", "function_call" ->
                "模型嘗試呼叫工具（未支援），未產生文字回覆$reason。"

            else -> if (hadReasoning) {
                "模型只產生了思考內容，未產生最終回覆$reason。請提高 Max Output Tokens，或將 Thinking 模式設為停用。"
            } else {
                "未收到任何回覆內容$reason。請確認模型名稱與帳號額度。"
            }
        }
    }

    override fun onCleared() {
        sendJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val DEFAULT_OUTPUT_RESERVE = 2048
        const val STREAM_UI_INTERVAL_MS = 50L
    }
}
