package com.example.llmchat.ui.conversations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.llmchat.domain.model.Conversation
import com.example.llmchat.ui.chat.ChatPane
import com.example.llmchat.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConversationsScreen(
    navController: NavController,
    expanded: Boolean,
    viewModel: ConversationViewModel = hiltViewModel()
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val visibleConversations by viewModel.visibleConversations.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val hasProfile by viewModel.hasProfile.collectAsStateWithLifecycle()
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
    val apiName = currentProfile?.name?.ifBlank { null }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val showNewButton = backStackEntry?.destination?.route == Routes.CONVERSATIONS
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var renameTarget by remember { mutableStateOf<Conversation?>(null) }
    var deleteTarget by remember { mutableStateOf<Conversation?>(null) }

    fun openConversation(id: Long) {
        if (expanded) {
            selectedId = id
        } else {
            val title = conversations.firstOrNull { it.id == id }?.title
            navController.navigate(Routes.chat(id, title))
        }
    }

    fun newConversation() {
        if (!hasProfile) {
            navController.navigate(Routes.PROFILES)
            return
        }
        openConversation(Routes.NEW_CONVERSATION_ID)
    }

    if (expanded) {
        Row(modifier = Modifier.fillMaxSize()) {
            ConversationListPane(
                modifier = Modifier.width(340.dp).fillMaxHeight(),
                applyWindowInsets = true,
                showFab = showNewButton,
                apiName = apiName,
                conversations = conversations,
                visibleConversations = visibleConversations,
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onClearSearch = viewModel::clearSearchQuery,
                selectedId = selectedId,
                onOpen = ::openConversation,
                onNew = ::newConversation,
                onRename = { renameTarget = it },
                onDelete = { deleteTarget = it },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
            VerticalDivider()
            Box(modifier = Modifier.weight(1f)) {
                val id = selectedId
                if (id != null) {
                    ChatPane(
                        conversationId = id,
                        onBack = null,
                        initialTitle = conversations.firstOrNull { it.id == id }?.title
                    )
                } else {
                    EmptyDetailPlaceholder()
                }
            }
        }
    } else {
        ConversationListPane(
            modifier = Modifier.fillMaxSize(),
            applyWindowInsets = false,
            showFab = showNewButton,
            apiName = apiName,
            conversations = conversations,
            visibleConversations = visibleConversations,
            searchQuery = searchQuery,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onClearSearch = viewModel::clearSearchQuery,
            selectedId = null,
            onOpen = ::openConversation,
            onNew = ::newConversation,
            onRename = { renameTarget = it },
            onDelete = { deleteTarget = it },
            onOpenSettings = { navController.navigate(Routes.SETTINGS) }
        )
    }

    renameTarget?.let { target ->
        RenameDialog(
            initial = target.title,
            onConfirm = {
                viewModel.renameConversation(target.id, it)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("刪除對話") },
            text = { Text("確定要刪除「${target.title}」嗎？此操作無法復原。") },
            confirmButton = {
                TextButton(onClick = {
                    if (selectedId == target.id) selectedId = null
                    viewModel.deleteConversation(target.id)
                    deleteTarget = null
                }) { Text("刪除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationListPane(
    modifier: Modifier,
    applyWindowInsets: Boolean,
    showFab: Boolean,
    apiName: String?,
    conversations: List<Conversation>,
    visibleConversations: List<Conversation>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    selectedId: Long?,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    onRename: (Conversation) -> Unit,
    onDelete: (Conversation) -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = if (applyWindowInsets) {
            ScaffoldDefaults.contentWindowInsets
        } else {
            WindowInsets(0, 0, 0, 0)
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("對話")
                        apiName?.let { name ->
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "設定")
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(200))
            ) {
                FloatingActionButton(onClick = onNew) {
                    Icon(Icons.Filled.Add, contentDescription = "新增對話")
                }
            }
        }
    ) { padding ->
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                    Spacer(Modifier.height(8.dp))
                    Text("還沒有對話")
                    Text(
                        "點右下角 + 開始新的對話",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "search") {
                    ConversationSearchField(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onClear = onClearSearch
                    )
                }
                if (visibleConversations.isEmpty()) {
                    item(key = "no_match") {
                        Text(
                            text = "找不到符合的對話",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(visibleConversations, key = { it.id }) { conversation ->
                        ConversationCard(
                            conversation = conversation,
                            selected = conversation.id == selectedId,
                            onOpen = { onOpen(conversation.id) },
                            onRename = { onRename(conversation) },
                            onDelete = { onDelete(conversation) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("搜尋對話標題") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Close, contentDescription = "清除查詢")
                }
            }
        },
        singleLine = true
    )
}

@Composable
private fun ConversationCard(
    conversation: Conversation,
    selected: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatTimestamp(conversation.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Filled.Edit, contentDescription = "重新命名")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "刪除")
            }
        }
    }
}

@Composable
private fun EmptyDetailPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "選擇一個對話，或新增對話",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RenameDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重新命名") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("對話名稱") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text("確定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

private fun formatTimestamp(millis: Long): String = timestampFormat.format(Date(millis))
