package com.aiworkgroup.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aiworkgroup.data.api.ChatMessage
import com.aiworkgroup.domain.DiscussionMessage
import com.aiworkgroup.ui.viewmodel.WorkFlowStep
import com.aiworkgroup.ui.viewmodel.WorkGroupViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkGroupScreenContent(
    viewModel: WorkGroupViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val discussionMessages by viewModel.discussionMessages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("") }
    var discussionTopic by remember { mutableStateOf("") }
    var showDiscussion by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size, discussionMessages.size) {
        scope.launch {
            listState.animateScrollToItem(
                if (showDiscussion) discussionMessages.size else messages.size
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 工作组信息卡片
        uiState.workGroup?.let { group ->
            WorkGroupInfoCard(group)
        }

        // 消息列表
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showDiscussion) {
                items(discussionMessages) { msg ->
                    DiscussionMessageItem(msg)
                }
            } else {
                items(messages) { msg ->
                    ChatMessageItem(msg)
                }
            }

            if (uiState.isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // 快捷操作按钮
        if (!showDiscussion && uiState.workGroup != null) {
            QuickActionBar(uiState, viewModel)
        }

        // 输入区域
        if (showDiscussion && uiState.currentStep == WorkFlowStep.DISCUSSION) {
            DiscussionInputBar(
                topic = discussionTopic,
                onTopicChange = { discussionTopic = it },
                onStartDiscussion = {
                    viewModel.startDiscussion(discussionTopic, 3)
                },
                isDiscussing = uiState.isDiscussing
            )
        } else {
            ChatInputBar(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendUserMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = !uiState.isLoading
            )
        }
    }

    // 错误提示
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("❌ 错误") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
fun WorkGroupInfoCard(group: com.aiworkgroup.domain.WorkGroup) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "📋 ${group.name}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                group.roles.forEach { role ->
                    AssistChip(
                        onClick = { },
                        label = { Text("${role.avatar} ${role.name}") },
                        colors = AssistChipDefaults.assistChipColors(
                            leadingIconContentColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            )
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = if (isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DiscussionMessageItem(message: DiscussionMessage) {
    val isSystem = message.roleId == "system"

    if (isSystem) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = message.content,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = message.avatar,
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Column {
                Text(
                    text = message.roleName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Card(
                    modifier = Modifier.padding(top = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionBar(
    uiState: com.aiworkgroup.ui.viewmodel.WorkGroupUiState,
    viewModel: WorkGroupViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (uiState.currentStep) {
            WorkFlowStep.ASSIGN_TASKS -> {
                Button(
                    onClick = { viewModel.assignTasks() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Assignment, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("开始分工")
                }
            }
            WorkFlowStep.DISCUSSION -> {
                Button(
                    onClick = { viewModel.startDiscussion("基于任务进行讨论", 3) },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isDiscussing
                ) {
                    Icon(Icons.Default.Forum, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("开始讨论")
                }
            }
            WorkFlowStep.FINAL_PLAN -> {
                Button(
                    onClick = { viewModel.generateFinalPlan() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("生成方案")
                }
            }
            else -> {}
        }
    }
}

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入任务描述或消息...") },
                enabled = enabled,
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = onSend,
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Send, contentDescription = "发送")
            }
        }
    }
}

@Composable
fun DiscussionInputBar(
    topic: String,
    onTopicChange: (String) -> Unit,
    onStartDiscussion: () -> Unit,
    isDiscussing: Boolean
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (!isDiscussing) {
                OutlinedTextField(
                    value = topic,
                    onValueChange = onTopicChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入讨论主题...") },
                    label = { Text("讨论主题") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onStartDiscussion,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = topic.isNotBlank()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("开始多AI讨论")
                }
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    "AI们正在讨论中，请稍候...",
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
