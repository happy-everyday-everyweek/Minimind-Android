package com.minimind.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minimind.app.backend.BackendManager
import com.minimind.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val apiProvider by viewModel.apiProvider.collectAsState()
    val apiBase by viewModel.apiBase.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val apiModel by viewModel.apiModel.collectAsState()
    val backendStatus by viewModel.backendStatus.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val isRestarting by viewModel.isRestarting.collectAsState()

    var apiProviderExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "LLM API 配置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box {
                        OutlinedTextField(
                            value = when (apiProvider) {
                                "deepseek" -> "DeepSeek"
                                "zhipu" -> "智谱"
                                else -> "自定义"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("API 提供商预设") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { apiProviderExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }
                        )
                        DropdownMenu(expanded = apiProviderExpanded, onDismissRequest = { apiProviderExpanded = false }) {
                            DropdownMenuItem(text = { Text("DeepSeek") }, onClick = {
                                viewModel.updateApiProvider("deepseek")
                                viewModel.updateApiBase("https://api.deepseek.com/v1")
                                viewModel.updateApiModel("deepseek-chat")
                                apiProviderExpanded = false
                            })
                            DropdownMenuItem(text = { Text("智谱") }, onClick = {
                                viewModel.updateApiProvider("zhipu")
                                viewModel.updateApiBase("https://open.bigmodel.cn/api/paas/v4")
                                viewModel.updateApiModel("glm-4")
                                apiProviderExpanded = false
                            })
                            DropdownMenuItem(text = { Text("自定义") }, onClick = {
                                viewModel.updateApiProvider("custom")
                                apiProviderExpanded = false
                            })
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiBase,
                        onValueChange = { viewModel.updateApiBase(it) },
                        label = { Text("API 地址") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { viewModel.updateApiKey(it) },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiModel,
                        onValueChange = { viewModel.updateApiModel(it) },
                        label = { Text("模型名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.testConnection() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isTesting,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("测试连接")
                    }

                    testResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (result.startsWith("连接成功")) SuccessGreen.copy(alpha = 0.1f)
                            else ErrorRed.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = result,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (result.startsWith("连接成功")) SuccessGreen else ErrorRed
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "后端服务管理",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("服务状态", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when (backendStatus) {
                                        BackendManager.BackendStatus.ONLINE -> SuccessGreen
                                        BackendManager.BackendStatus.OFFLINE -> ErrorRed
                                        BackendManager.BackendStatus.ERROR -> ErrorRed
                                        BackendManager.BackendStatus.CHECKING -> WarningOrange
                                        BackendManager.BackendStatus.UNKNOWN -> WarningOrange
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (backendStatus) {
                                BackendManager.BackendStatus.ONLINE -> "在线"
                                BackendManager.BackendStatus.OFFLINE -> "离线"
                                BackendManager.BackendStatus.ERROR -> "异常"
                                BackendManager.BackendStatus.CHECKING -> "检查中"
                                BackendManager.BackendStatus.UNKNOWN -> "未知"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (backendStatus) {
                                BackendManager.BackendStatus.ONLINE -> SuccessGreen
                                BackendManager.BackendStatus.OFFLINE -> ErrorRed
                                BackendManager.BackendStatus.ERROR -> ErrorRed
                                else -> WarningOrange
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.checkBackendStatus() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("检查状态")
                        }
                        OutlinedButton(
                            onClick = { viewModel.restartBackend() },
                            modifier = Modifier.weight(1f),
                            enabled = !isRestarting
                        ) {
                            if (isRestarting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("重启服务")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.reinitialize() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("重新初始化")
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "关于",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "MiniMind",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "版本 1.0.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "轻量级大语言模型训练与推理平台，支持预训练、SFT、知识蒸馏、LoRA、强化学习等多种训练方式。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
