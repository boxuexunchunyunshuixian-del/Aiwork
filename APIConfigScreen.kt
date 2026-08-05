package com.aiworkgroup.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiworkgroup.config.AIProvider
import com.aiworkgroup.config.AIProviderConfig
import com.aiworkgroup.ui.viewmodel.ConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun APIConfigScreen(
    onBack: () -> Unit,
    viewModel: ConfigViewModel = viewModel()
) {
    val configs by viewModel.configs.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("🔑 AI API 配置", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.saveConfigs() },
                icon = { Icon(Icons.Default.Save, contentDescription = null) },
                text = { Text("保存配置") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoCard()

            configs.forEach { config ->
                ProviderConfigCard(
                    config = config,
                    onConfigChange = { viewModel.updateConfig(it) }
                )
            }

            AnimatedVisibility(visible = isSaved) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("配置已保存！", color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "💡 配置说明",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "• 填写至少一个 AI 的 API Key 即可使用
" +
                "• 支持 OpenAI、Gemini、Claude、DeepSeek 等主流平台
" +
                "• 可自定义 BaseURL（用于代理或私有化部署）
" +
                "• 密钥将加密存储在本地，不会上传到服务器",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun ProviderConfigCard(
    config: AIProviderConfig,
    onConfigChange: (AIProviderConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(config.apiKey.isNotBlank() || config.provider == AIProvider.OPENAI) }
    var showKey by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (config.isEnabled && config.apiKey.isNotBlank())
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        getProviderEmoji(config.provider),
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            config.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            if (config.apiKey.isNotBlank()) "✅ 已配置" else "❌ 未配置",
                            fontSize = 12.sp,
                            color = if (config.apiKey.isNotBlank())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = config.isEnabled,
                        onCheckedChange = {
                            onConfigChange(config.copy(isEnabled = it))
                        }
                    )
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "收起" else "展开"
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = config.apiKey,
                        onValueChange = { onConfigChange(config.copy(apiKey = it)) },
                        label = { Text("API Key *") },
                        placeholder = { Text("sk-... 或 其他格式密钥") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showKey)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showKey) "隐藏" else "显示"
                                )
                            }
                        },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = config.modelName,
                        onValueChange = { onConfigChange(config.copy(modelName = it)) },
                        label = { Text("模型名称（可选）") },
                        placeholder = { Text("默认: ${config.defaultModel}") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = config.baseUrl,
                        onValueChange = { onConfigChange(config.copy(baseUrl = it)) },
                        label = { Text("Base URL（可选）") },
                        placeholder = { Text("默认: ${config.defaultBaseUrl}") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = config.timeoutSeconds.toString(),
                            onValueChange = {
                                it.toIntOrNull()?.let { sec ->
                                    onConfigChange(config.copy(timeoutSeconds = sec))
                                }
                            },
                            label = { Text("超时(秒)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = config.maxTokens.toString(),
                            onValueChange = {
                                it.toIntOrNull()?.let { tokens ->
                                    onConfigChange(config.copy(maxTokens = tokens))
                                }
                            },
                            label = { Text("Max Tokens") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}

fun getProviderEmoji(provider: AIProvider): String = when(provider) {
    AIProvider.OPENAI -> "🔷"
    AIProvider.GEMINI -> "🔶"
    AIProvider.CLAUDE -> "🔴"
    AIProvider.DEEPSEEK -> "🔵"
    AIProvider.CUSTOM -> "⚙️"
}
