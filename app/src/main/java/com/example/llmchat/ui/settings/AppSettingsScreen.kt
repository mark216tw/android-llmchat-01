package com.example.llmchat.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.llmchat.domain.model.ThemeMode
import com.example.llmchat.domain.model.TtsLanguage
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    navController: NavController,
    viewModel: AppSettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val fontSize by viewModel.chatFontSize.collectAsStateWithLifecycle()
    val alwaysRead by viewModel.alwaysRead.collectAsStateWithLifecycle()
    val ttsLanguage by viewModel.ttsLanguage.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "顯示模式",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            ThemeMode.entries.forEach { mode ->
                ThemeModeOption(
                    label = themeModeLabel(mode),
                    selected = themeMode == mode,
                    onSelect = { viewModel.setThemeMode(mode) }
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text(
                text = "字型大小",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            FontSizeSetting(
                fontSize = fontSize,
                onCommit = viewModel::setChatFontSize
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text(
                text = "朗讀",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "總是朗讀",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "啟用後，LLM 每次回覆完成都會自動朗讀。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = alwaysRead,
                    onCheckedChange = viewModel::setAlwaysRead
                )
            }

            Spacer(Modifier.height(16.dp))
            TtsLanguageSetting(
                selected = ttsLanguage,
                onSelect = viewModel::setTtsLanguage
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TtsLanguageSetting(
    selected: TtsLanguage,
    onSelect: (TtsLanguage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = ttsLanguageLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text("朗讀語言") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TtsLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(ttsLanguageLabel(language)) },
                    onClick = {
                        onSelect(language)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun ttsLanguageLabel(language: TtsLanguage): String = when (language) {
    TtsLanguage.SYSTEM -> "系統預設"
    TtsLanguage.ZH -> "中文（台灣）"
    TtsLanguage.EN -> "English"
}

@Composable
private fun ThemeModeOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FontSizeSetting(
    fontSize: Float,
    onCommit: (Float) -> Unit
) {
    var sliderValue by remember(fontSize) { mutableStateOf(fontSize) }
    val rounded = sliderValue.roundToInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onCommit(sliderValue) },
            valueRange = 12f..24f,
            steps = 11,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = "${rounded}sp",
            style = MaterialTheme.typography.bodyMedium
        )
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = "訊息預覽：這是一段範例文字。",
        fontSize = sliderValue.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
}

private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "系統預設"
    ThemeMode.LIGHT -> "淺色"
    ThemeMode.DARK -> "深色"
}
