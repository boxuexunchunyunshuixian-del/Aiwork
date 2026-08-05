package com.aiworkgroup.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aiworkgroup.ui.viewmodel.WorkGroupViewModel

@Composable
fun AIWorkGroupApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                onNavigateToConfig = { navController.navigate("config") }
            )
        }
        composable("config") {
            APIConfigScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToConfig: () -> Unit,
    viewModel: WorkGroupViewModel = viewModel()
) {
    val hasService by viewModel.hasAvailableService.collectAsStateWithLifecycle(initialValue = false)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("🤖 AI 工作组协作", fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = onNavigateToConfig) {
                        BadgedBox(
                            badge = {
                                if (!hasService) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "API配置")
                        }
                    }
                    if (uiState.workGroup != null) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "重新开始")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (!hasService && uiState.workGroup == null) {
                EmptyConfigGuide(onNavigateToConfig)
            } else {
                WorkGroupScreenContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun EmptyConfigGuide(onNavigateToConfig: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔑", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "请先配置 AI API",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "您需要至少配置一个 AI 提供商的 API 密钥，
" +
            "才能创建工作组并调用 AI 进行协作。",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNavigateToConfig,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("前往配置")
        }
    }
}
