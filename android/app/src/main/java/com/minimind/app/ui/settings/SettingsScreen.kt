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
import androidx.compose.ui.graphics.Color
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
    val apiBase by viewModel.apiBase.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val apiModel by viewModel.apiModel.collectAsState()
    val resourceLimits by viewModel.resourceLimits.collectAsState()
    val isSavingLimits by viewModel.isSavingLimits.collectAsState()
    val saveLimitsResult by viewModel.saveLimitsResult.collectAsState()
    val backendStatus by viewModel.backendStatus.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val isRestarting by viewModel.isRestarting.collectAsState()

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
                text = "外部模型 API 配置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "配置一个兼容 OpenAI API 格式的大语言模型接口，用于知识蒸馏和 AI 辅助生成奖励函数。你需要提供 API 的访问地址、密钥和模型名称。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
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
                            color = if (result.startsWith("连接成功")) Color(0xFF333333).copy(alpha = 0.1f)
                            else Color(0xFF888888).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = result,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (result.startsWith("连接成功")) Color(0xFF333333) else Color(0xFF888888)
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "资源限制",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "设置后端服务可以使用的系统资源上限，防止训练任务占用过多资源导致手机卡顿。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "CPU 使用率上限: ${resourceLimits.maxCpuPercent}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = resourceLimits.maxCpuPercent.toFloat(),
                        onValueChange = { viewModel.updateMaxCpuPercent(it.toInt()) },
                        valueRange = 0f..100f,
                        steps = 20,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = if (resourceLimits.maxMemoryMb == 0) "" else resourceLimits.maxMemoryMb.toString(),
                        onValueChange = { viewModel.updateMaxMemoryMb(it.toIntOrNull() ?: 0) },
                        label = { Text("内存使用上限 (MB)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = if (resourceLimits.maxTrainingProcesses == 0) "" else resourceLimits.maxTrainingProcesses.toString(),
                        onValueChange = { viewModel.updateMaxTrainingProcesses(it.toIntOrNull() ?: 0) },
                        label = { Text("最大同时训练进程数") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.saveResourceLimits() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSavingLimits,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isSavingLimits) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("保存")
                    }

                    saveLimitsResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (result.startsWith("保存成功")) Color(0xFF333333).copy(alpha = 0.1f)
                            else Color(0xFF888888).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = result,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (result.startsWith("保存成功")) Color(0xFF333333) else Color(0xFF888888)
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
                                        BackendManager.BackendStatus.ONLINE -> Color(0xFF333333)
                                        BackendManager.BackendStatus.OFFLINE -> Color(0xFFBBBBBB)
                                        BackendManager.BackendStatus.ERROR -> Color(0xFFBBBBBB)
                                        BackendManager.BackendStatus.CHECKING -> Color(0xFF888888)
                                        BackendManager.BackendStatus.UNKNOWN -> Color(0xFF888888)
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
                                BackendManager.BackendStatus.ONLINE -> Color(0xFF333333)
                                BackendManager.BackendStatus.OFFLINE -> Color(0xFFBBBBBB)
                                BackendManager.BackendStatus.ERROR -> Color(0xFFBBBBBB)
                                else -> Color(0xFF888888)
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
                            tint = PrimaryVariant
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
