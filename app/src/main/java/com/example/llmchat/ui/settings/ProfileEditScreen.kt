package com.example.llmchat.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.llmchat.domain.model.Profile
import com.example.llmchat.domain.model.ReasoningEffort
import com.example.llmchat.domain.model.ThinkingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    navController: NavController,
    profileId: Long,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val testState by viewModel.testState.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var systemPrompt by remember { mutableStateOf("") }
    var temperature by remember { mutableFloatStateOf(0.7f) }
    var maxTokens by remember { mutableIntStateOf(0) }
    var contextWindow by remember { mutableIntStateOf(8192) }
    var thinkingMode by remember { mutableStateOf(ThinkingMode.DEFAULT) }
    var reasoningEffort by remember { mutableStateOf(ReasoningEffort.DEFAULT) }
    var apiKeyVisible by remember { mutableStateOf(false) }

    LaunchedEffect(profileId) {
        viewModel.resetTestState()
        if (profileId > 0L) {
            viewModel.getProfile(profileId)?.let { p ->
                name = p.name
                baseUrl = p.baseUrl
                apiKey = p.apiKey
                model = p.model
                systemPrompt = p.systemPrompt
                temperature = p.temperature.toFloat()
                maxTokens = p.maxTokens
                contextWindow = p.contextWindow
                thinkingMode = p.thinkingMode
                reasoningEffort = p.reasoningEffort
            }
        }
    }

    fun currentProfile(): Profile = Profile(
        id = profileId.coerceAtLeast(0L),
        name = name.trim().ifBlank { "新設定" },
        baseUrl = baseUrl.trim(),
        apiKey = apiKey.trim(),
        model = model.trim(),
        systemPrompt = systemPrompt,
        temperature = temperature.toDouble(),
        maxTokens = maxTokens,
        contextWindow = contextWindow,
        thinkingMode = thinkingMode,
        reasoningEffort = reasoningEffort
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (profileId > 0L) "編輯設定" else "新增設定") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.save(currentProfile()) { navController.popBackStack() }
                    }) {
                        Icon(Icons.Filled.Check, contentDescription = "儲存")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("設定名稱") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL") },
                placeholder = { Text("https://api.openai.com/v1") },
                supportingText = { Text("OpenAI 相容端點，通常以 /v1 結尾") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (apiKeyVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                        Icon(
                            if (apiKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (apiKeyVisible) "隱藏" else "顯示"
                        )
                    }
                }
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("模型名稱（必填）") },
                placeholder = { Text("gpt-4o-mini") },
                supportingText = { Text("實際使用的模型 id，需與 /models 回報的 id 一致") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            val availableModels = (testState as? ConnectionTestState.Success)?.models.orEmpty()
            if (availableModels.isNotEmpty()) {
                ModelPicker(
                    models = availableModels,
                    selected = model,
                    onSelect = { model = it }
                )
            } else if (testState == ConnectionTestState.Idle) {
                Text(
                    text = "尚未測試連線：此欄即最終使用的模型；按「測試連線」成功後會顯示模型清單可直接挑選，選取後同樣填入此欄。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (testState is ConnectionTestState.Error) {
                Text(
                    text = "無法取得模型清單（部分端點未提供 /models）：仍可直接在上方手動輸入模型名稱，對話照常可用。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Button(
                    onClick = { viewModel.testConnection(baseUrl.trim(), apiKey.trim()) },
                    enabled = testState != ConnectionTestState.Loading
                ) {
                    Text("測試連線")
                }
                Spacer(Modifier.width(12.dp))
                ConnectionTestIndicator(testState)
            }

            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("System Prompt（可留空）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Text("Temperature：${"%.2f".format(temperature)}")
            Slider(
                value = temperature,
                onValueChange = { temperature = it },
                valueRange = 0f..2f,
                steps = 19
            )

            NumberField(
                label = "Max Output Tokens（0 = 不指定，由伺服器決定）",
                value = maxTokens,
                onValueChange = { maxTokens = it }
            )
            NumberField(
                label = "Context Window（模型上下文長度）",
                value = contextWindow,
                onValueChange = { contextWindow = it },
                supportingText = "超過長度上限時，會自動從最舊的訊息開始省略（系統提示與較新內容優先保留；完整歷史仍保存在本機）。"
            )

            OptionDropdown(
                label = "Thinking 模式（DeepSeek）",
                options = ThinkingMode.entries,
                selected = thinkingMode,
                optionLabel = { mode ->
                    when (mode) {
                        ThinkingMode.DEFAULT -> "預設（不指定）"
                        ThinkingMode.ENABLED -> "啟用思考"
                        ThinkingMode.DISABLED -> "停用思考"
                    }
                },
                onSelect = { thinkingMode = it }
            )
            OptionDropdown(
                label = "Reasoning Effort",
                options = ReasoningEffort.entries,
                selected = reasoningEffort,
                optionLabel = { effort ->
                    when (effort) {
                        ReasoningEffort.DEFAULT -> "預設（不指定）"
                        ReasoningEffort.NONE -> "none"
                        ReasoningEffort.LOW -> "low"
                        ReasoningEffort.HIGH -> "high"
                        ReasoningEffort.MAX -> "max"
                    }
                },
                onSelect = { reasoningEffort = it }
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.save(currentProfile()) { navController.popBackStack() }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("儲存")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPicker(
    models: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected.ifBlank { "請選擇模型" },
            onValueChange = {},
            readOnly = true,
            label = { Text("從清單選擇模型") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            models.forEach { id ->
                DropdownMenuItem(
                    text = { Text(id) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> OptionDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { text ->
            val parsed = text.filter { it.isDigit() }.toIntOrNull()
            if (parsed != null) onValueChange(parsed) else if (text.isEmpty()) onValueChange(0)
        },
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}
