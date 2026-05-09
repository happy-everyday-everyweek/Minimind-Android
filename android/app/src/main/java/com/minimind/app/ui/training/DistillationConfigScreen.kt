package com.minimind.app.ui.training

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.minimind.app.network.ApiClient
import com.minimind.app.network.model.DistillationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistillationConfigScreen(onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var apiProvider by remember { mutableStateOf("deepseek") }
    var apiProviderExpanded by remember { mutableStateOf(false) }
    var teacherApiBase by remember { mutableStateOf("https://api.deepseek.com/v1") }
    var teacherApiKey by remember { mutableStateOf("") }
    var teacherModelName by remember { mutableStateOf("deepseek-chat") }
    var studentWeight by remember { mutableStateOf("full_sft") }
    var studentWeightExpanded by remember { mutableStateOf(false) }
    var dataset by remember { mutableStateOf("distill_data") }
    var learningRate by remember { mutableStateOf("1e-5") }
    var batchSize by remember { mutableStateOf("16") }
    var epochs by remember { mutableStateOf("2") }
    var isStarting by remember { mutableStateOf(false) }
    var startResult by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("知识蒸馏配置") },
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "教师模型 API 配置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Box {
                OutlinedTextField(
                    value = when (apiProvider) {
                        "deepseek" -> "DeepSeek"
                        "zhipu" -> "智谱"
                        else -> "自定义"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("API 提供商") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { apiProviderExpanded = true }) {
                            Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown, null)
                        }
                    }
                )
                DropdownMenu(expanded = apiProviderExpanded, onDismissRequest = { apiProviderExpanded = false }) {
                    DropdownMenuItem(text = { Text("DeepSeek") }, onClick = {
                        apiProvider = "deepseek"
                        teacherApiBase = "https://api.deepseek.com/v1"
                        teacherModelName = "deepseek-chat"
                        apiProviderExpanded = false
                    })
                    DropdownMenuItem(text = { Text("智谱") }, onClick = {
                        apiProvider = "zhipu"
                        teacherApiBase = "https://open.bigmodel.cn/api/paas/v4"
                        teacherModelName = "glm-4"
                        apiProviderExpanded = false
                    })
                    DropdownMenuItem(text = { Text("自定义") }, onClick = {
                        apiProvider = "custom"
                        apiProviderExpanded = false
                    })
                }
            }
            OutlinedTextField(
                value = teacherApiBase,
                onValueChange = { teacherApiBase = it },
                label = { Text("API 地址") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = teacherApiKey,
                onValueChange = { teacherApiKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            OutlinedTextField(
                value = teacherModelName,
                onValueChange = { teacherModelName = it },
                label = { Text("模型名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                text = "学生模型配置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Box {
                OutlinedTextField(
                    value = studentWeight,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("学生模型权重") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { studentWeightExpanded = true }) {
                            Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown, null)
                        }
                    }
                )
                DropdownMenu(expanded = studentWeightExpanded, onDismissRequest = { studentWeightExpanded = false }) {
                    DropdownMenuItem(text = { Text("full_sft") }, onClick = { studentWeight = "full_sft"; studentWeightExpanded = false })
                    DropdownMenuItem(text = { Text("pretrain") }, onClick = { studentWeight = "pretrain"; studentWeightExpanded = false })
                }
            }

            Text(
                text = "训练参数",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = dataset,
                onValueChange = { dataset = it },
                label = { Text("蒸馏数据集") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = learningRate,
                onValueChange = { learningRate = it },
                label = { Text("学习率") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = batchSize,
                onValueChange = { batchSize = it },
                label = { Text("批次大小") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = epochs,
                onValueChange = { epochs = it },
                label = { Text("训练轮数") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            startResult?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (it.startsWith("成功")) com.minimind.app.ui.theme.SuccessGreen.copy(alpha = 0.1f)
                        else com.minimind.app.ui.theme.ErrorRed.copy(alpha = 0.1f)
                    )
                ) {
                    Text(text = it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isStarting = true
                        try {
                            val config = DistillationConfig(
                                teacherApiBase = teacherApiBase,
                                teacherApiKey = teacherApiKey,
                                teacherModelName = teacherModelName,
                                studentWeight = studentWeight,
                                dataPath = dataset,
                                learningRate = learningRate.toFloatOrNull() ?: 1e-5f,
                                batchSize = batchSize.toIntOrNull() ?: 16,
                                epochs = epochs.toIntOrNull() ?: 2
                            )
                            val response = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                ApiClient.apiService.startDistillation(config)
                            }
                            startResult = "成功启动蒸馏，任务ID: ${response.taskId}"
                        } catch (e: Exception) {
                            startResult = "启动失败: ${e.message}"
                        }
                        isStarting = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isStarting,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isStarting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("开始蒸馏")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
