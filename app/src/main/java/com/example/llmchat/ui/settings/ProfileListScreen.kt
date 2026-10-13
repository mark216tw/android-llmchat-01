package com.example.llmchat.ui.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.llmchat.domain.model.Profile
import com.example.llmchat.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    navController: NavController,
    applyWindowInsets: Boolean = true,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val currentProfileId by viewModel.currentProfileId.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Profile?>(null) }

    Scaffold(
        contentWindowInsets = if (applyWindowInsets) {
            ScaffoldDefaults.contentWindowInsets
        } else {
            WindowInsets(0, 0, 0, 0)
        },
        topBar = {
            TopAppBar(
                title = { Text("API 設定") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.DEBUG) }) {
                        Icon(Icons.Filled.BugReport, contentDescription = "除錯記錄")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.profileEdit(-1L)) }) {
                Icon(Icons.Filled.Add, contentDescription = "新增設定")
            }
        }
    ) { padding ->
        if (profiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = null,
                        modifier = Modifier.height(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("尚未新增任何 API 設定")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "點右下角 + 新增，填入 Base URL、API Key 與模型名稱",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileCard(
                        profile = profile,
                        selected = profile.id == currentProfileId,
                        onSelect = { viewModel.selectCurrent(profile.id) },
                        onEdit = { navController.navigate(Routes.profileEdit(profile.id)) },
                        onDelete = { pendingDelete = profile }
                    )
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("刪除設定") },
            text = { Text("確定要刪除「${target.name}」嗎？相關的對話也會一併刪除。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target.id)
                    pendingDelete = null
                }) { Text("刪除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: Profile,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
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
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name.ifBlank { "(未命名)" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${profile.baseUrl}  ·  ${profile.model}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            if (selected) {
                Text(
                    "使用中",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "刪除")
            }
        }
    }
}

@Composable
fun ConnectionTestIndicator(state: ConnectionTestState) {
    when (state) {
        ConnectionTestState.Idle -> Unit
        ConnectionTestState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.height(18.dp).width(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("測試連線中…", style = MaterialTheme.typography.bodySmall)
        }

        is ConnectionTestState.Success -> Text(
            "連線成功，取得 ${state.models.size} 個模型",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall
        )

        is ConnectionTestState.Error -> Text(
            "連線失敗：${state.message}",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
