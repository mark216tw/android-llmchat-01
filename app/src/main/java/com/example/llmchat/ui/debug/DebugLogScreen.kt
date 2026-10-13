package com.example.llmchat.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.llmchat.debug.DebugLogEntry
import com.example.llmchat.debug.LogLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogScreen(
    navController: NavController,
    viewModel: DebugLogViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("除錯記錄") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(formatAll(entries))) },
                        enabled = entries.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "複製全部")
                    }
                    IconButton(
                        onClick = { showClearConfirm = true },
                        enabled = entries.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "清空")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("啟用記錄", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "關閉後不再寫入新的除錯記錄",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = enabled, onCheckedChange = viewModel::setEnabled)
            }
            HorizontalDivider()

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (enabled) "尚無記錄\n送出訊息後再回來查看" else "記錄已關閉",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val newestFirst = remember(entries) { entries.asReversed() }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(newestFirst, key = { it.id }) { entry ->
                        DebugLogRow(entry)
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空記錄") },
            text = { Text("確定要清空所有除錯記錄嗎？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clear()
                    showClearConfirm = false
                }) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun DebugLogRow(entry: DebugLogEntry) {
    val levelColor = when (entry.level) {
        LogLevel.REQUEST -> MaterialTheme.colorScheme.primary
        LogLevel.EVENT -> MaterialTheme.colorScheme.onSurfaceVariant
        LogLevel.RESPONSE -> MaterialTheme.colorScheme.tertiary
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
        LogLevel.INFO -> MaterialTheme.colorScheme.secondary
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${entry.time}  ${entry.level.name}",
            color = levelColor,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = entry.message,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

private fun formatAll(entries: List<DebugLogEntry>): String =
    entries.joinToString(separator = "\n") { "[${it.time}] ${it.level.name}: ${it.message}" }
