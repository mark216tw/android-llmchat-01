package com.example.llmchat.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.llmchat.domain.model.ChatRole
import com.example.llmchat.domain.model.Message
import com.example.llmchat.ui.theme.LocalChatFontSize
import com.example.llmchat.ui.theme.LocalTtsLocale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPane(
    conversationId: Long,
    onBack: (() -> Unit)?,
    initialTitle: String? = null,
    viewModel: ChatViewModel = hiltViewModel(key = "chat_$conversationId")
) {
    LaunchedEffect(conversationId) { viewModel.init(conversationId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<Message?>(null) }
    var menuTarget by remember { mutableStateOf<Message?>(null) }
    val clipboard = LocalClipboardManager.current
    val chatFontSize = LocalChatFontSize.current
    val ttsLocale = LocalTtsLocale.current
    val conversationTitle = state.conversation?.title ?: initialTitle?.ifBlank { null } ?: "新對話"
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    // One controller per chat screen, reused across conversation switches.
    val speech = remember { SpeechController(context) }
    DisposableEffect(Unit) {
        onDispose { speech.shutdown() }
    }
    val speechState by speech.state.collectAsStateWithLifecycle()
    LaunchedEffect(conversationId) {
        speech.stop()
        viewModel.autoReadEvents.collect { event ->
            speech.start(event.messageId, markdownToPlainText(event.text), ttsLocale)
        }
    }
    LaunchedEffect(Unit) {
        speech.errors.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(state.isStreaming) {
        if (state.isStreaming) speech.stop()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = conversationTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    if (speechState.messageId != null) {
                        IconButton(onClick = speech::togglePause) {
                            if (speechState.paused) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "繼續朗讀")
                            } else {
                                Icon(Icons.Filled.Pause, contentDescription = "暫停朗讀")
                            }
                        }
                        IconButton(onClick = speech::stop) {
                            Icon(Icons.Filled.Stop, contentDescription = "停止朗讀")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
        ) {
            if (state.omittedCount > 0 || state.estimatedTokens > 0) {
                TokenInfoBar(
                    omittedCount = state.omittedCount,
                    estimatedTokens = state.estimatedTokens,
                    fontSize = chatFontSize
                )
            }

            val listState = rememberLazyListState()
            val isAtBottom by remember { derivedStateOf { !listState.canScrollForward } }
            var didInitialScroll by remember(conversationId) { mutableStateOf(false) }
            LaunchedEffect(state.messages, state.isStreaming, state.streamingText) {
                if (state.messages.isEmpty()) return@LaunchedEffect
                val target = state.messages.size + if (state.isStreaming) 1 else 0
                if (!didInitialScroll) {
                    didInitialScroll = true
                    // Entering an existing conversation: pin the last question to the top.
                    if (!state.isStreaming) {
                        val lastUserIndex = state.messages.indexOfLast { it.role == ChatRole.USER }
                        if (lastUserIndex >= 0) {
                            listState.scrollToItem(lastUserIndex)
                            return@LaunchedEffect
                        }
                    }
                    if (target > 0) listState.scrollToItem(target - 1, Int.MAX_VALUE)
                    return@LaunchedEffect
                }
                // Follow new content only while the user is already at the bottom,
                // so reading earlier messages is not interrupted by streaming.
                if (target > 0 && isAtBottom) {
                    listState.scrollToItem(target - 1, Int.MAX_VALUE)
                }
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (state.isLoadingMessages) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.messages, key = { it.id }) { message ->
                            MessageBubble(
                                role = message.role,
                                content = message.content,
                                fontSize = chatFontSize,
                                highlighted = speechState.messageId == message.id,
                                createdAt = message.createdAt,
                                onLongClick = if (!state.isStreaming) {
                                    { menuTarget = message }
                                } else {
                                    null
                                }
                            ) {
                                MessageMenu(
                                    expanded = menuTarget?.id == message.id,
                                    isUser = message.role == ChatRole.USER,
                                    onDismiss = { menuTarget = null },
                                    onCopy = {
                                        clipboard.setText(AnnotatedString(message.content))
                                        menuTarget = null
                                        scope.launch { snackbarHostState.showSnackbar("已複製") }
                                    },
                                    onCopyWithTitle = {
                                        clipboard.setText(
                                            AnnotatedString(withTitle(conversationTitle, message.content))
                                        )
                                        menuTarget = null
                                        scope.launch { snackbarHostState.showSnackbar("已複製") }
                                    },
                                    onShareWithTitle = {
                                        shareText(context, withTitle(conversationTitle, message.content))
                                        menuTarget = null
                                    },
                                    onRead = {
                                        speech.start(message.id, markdownToPlainText(message.content), ttsLocale)
                                        menuTarget = null
                                        scope.launch { snackbarHostState.showSnackbar("開始朗讀") }
                                    },
                                    onReask = {
                                        viewModel.reask(message)
                                        menuTarget = null
                                    },
                                    onDelete = {
                                        deleteTarget = message
                                        menuTarget = null
                                    }
                                )
                            }
                        }
                        if (state.isStreaming && state.reasoningText.isNotEmpty()) {
                            item(key = "reasoning") {
                                ReasoningBlock(state.reasoningText)
                            }
                        }
                        if (state.isStreaming) {
                            item(key = "streaming") {
                                MessageBubble(
                                    role = ChatRole.ASSISTANT,
                                    content = state.streamingText.ifEmpty { "…" },
                                    fontSize = chatFontSize,
                                    markdown = false
                                )
                            }
                        }
                    }
                }

                if (!isAtBottom && state.messages.isNotEmpty()) {
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                val last = listState.layoutInfo.totalItemsCount - 1
                                if (last >= 0) listState.animateScrollToItem(last, Int.MAX_VALUE)
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "回到底部")
                    }
                }
            }

            state.error?.let { message ->
                ErrorBar(message = message, onDismiss = viewModel::clearError)
            }

            ChatInputRow(
                inputText = state.inputText,
                isStreaming = state.isStreaming,
                fontSize = chatFontSize,
                onInputChange = viewModel::onInputChange,
                onSend = viewModel::send,
                onStop = viewModel::stop
            )
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("刪除問答") },
            text = { Text("確定要刪除此問題及其回答嗎？此操作無法復原。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteQuestion(target)
                    deleteTarget = null
                }) { Text("刪除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}

@Composable
fun ChatScreen(
    navController: NavController,
    conversationId: Long,
    initialTitle: String? = null
) {
    ChatPane(
        conversationId = conversationId,
        onBack = { navController.popBackStack() },
        initialTitle = initialTitle
    )
}

@Composable
private fun TokenInfoBar(omittedCount: Int, estimatedTokens: Int, fontSize: TextUnit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = buildString {
                append("本次約 $estimatedTokens tokens")
                if (omittedCount > 0) {
                    append("｜已省略 $omittedCount 則早期訊息")
                }
            },
            style = MaterialTheme.typography.labelSmall.copy(fontSize = fontSize * 0.7f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    role: ChatRole,
    content: String,
    fontSize: TextUnit,
    markdown: Boolean = true,
    highlighted: Boolean = false,
    createdAt: Long? = null,
    onLongClick: (() -> Unit)? = null,
    menu: (@Composable () -> Unit)? = null
) {
    val isUser = role == ChatRole.USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = fontSize,
        lineHeight = fontSize * 1.4f
    )
    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(onClick = {}, onLongClick = onLongClick)
    } else {
        Modifier
    }
    val highlightModifier = if (highlighted) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
    } else {
        Modifier
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Box {
                Surface(
                    color = bubbleColor,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .then(highlightModifier)
                        .then(clickModifier)
                ) {
                    if (isUser || !markdown) {
                        Text(
                            text = content,
                            color = textColor,
                            style = textStyle,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    } else {
                        MarkdownText(
                            text = content,
                            color = textColor,
                            style = textStyle,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                menu?.invoke()
            }
            if (createdAt != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatMessageTime(createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageMenu(
    expanded: Boolean,
    isUser: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onCopyWithTitle: () -> Unit,
    onShareWithTitle: () -> Unit,
    onRead: () -> Unit,
    onReask: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (!isUser) {
            DropdownMenuItem(
                text = { Text("複製(含標題)") },
                leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                onClick = onCopyWithTitle
            )
            DropdownMenuItem(
                text = { Text("分享(含標題)") },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                onClick = onShareWithTitle
            )
            DropdownMenuItem(
                text = { Text("朗讀") },
                leadingIcon = { Icon(Icons.Filled.RecordVoiceOver, contentDescription = null) },
                onClick = onRead
            )
        }
        if (isUser) {
            DropdownMenuItem(
                text = { Text("複製") },
                leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                onClick = onCopy
            )
            DropdownMenuItem(
                text = { Text("再次發問") },
                leadingIcon = { Icon(Icons.Filled.Replay, contentDescription = null) },
                onClick = onReask
            )
            DropdownMenuItem(
                text = { Text("刪除") },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun ReasoningBlock(content: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = "思考中…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ErrorBar(message: String, onDismiss: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "關閉",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun ChatInputRow(
    inputText: String,
    isStreaming: Boolean,
    fontSize: TextUnit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("輸入訊息…") },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSize,
                    lineHeight = fontSize * 1.4f
                ),
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = { if (!isStreaming && inputText.isNotBlank()) onSend() }
                )
            )
            Spacer(Modifier.width(8.dp))
            if (isStreaming) {
                IconButton(onClick = onStop) {
                    Icon(Icons.Filled.Stop, contentDescription = "停止生成")
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = inputText.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "送出")
                }
            }
        }
    }
}

private val timeOfDayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dateTimeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
private val dayStampFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

private fun formatMessageTime(millis: Long): String {
    val date = Date(millis)
    val sameDay = dayStampFormat.format(date) == dayStampFormat.format(Date())
    return if (sameDay) timeOfDayFormat.format(date) else dateTimeFormat.format(date)
}

private fun withTitle(title: String, content: String): String =
    if (title.isBlank()) content else "$title\n\n$content"

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享"))
}
