package com.minimind.app.ui.training

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minimind.app.network.ApiClient
import com.minimind.app.network.model.SftConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SftConfigScreen(onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var fromWeight by remember { mutableStateOf("pretrain") }
    var fromWeightExpanded by remember { mutableStateOf(false) }
    var dataset by remember { mutableStateOf("sft_t2t") }
    var datasetExpanded by remember { mutableStateOf(false) }
    var learningRate by remember { mutableStateOf("1e-5") }
    var batchSize by remember { mutableStateOf("16") }
    var epochs by remember { mutableStateOf("2") }
    var maxSeqLen by remember { mutableStateOf("768") }
    var isStarting by remember { mutableStateOf(false) }
    var startResult by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SFT 配置") },
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "有监督微调是在预训练模型的基础上，用带有正确答案的对话数据来教模型如何与用户交互。预训练让模型学会了\"说话\"，SFT 则教模型学会\"对话\"。\n\n基础权重：选择预训练阶段产出的模型权重作为起点。数据集选择：SFT 数据包含了多轮对话、问答、工具调用等多种格式，模型会学习这些格式来更好地回应用户。",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Text(
                text = "基础权重",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Box {
                OutlinedTextField(
                    value = fromWeight,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("基础权重") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { fromWeightExpanded = true }) {
                            Icon(Icons.Filled.ArrowDropDown, null)
                        }
                    }
                )
                DropdownMenu(expanded = fromWeightExpanded, onDismissRequest = { fromWeightExpanded = false }) {
                    DropdownMenuItem(text = { Text("pretrain") }, onClick = { fromWeight = "pretrain"; fromWeightExpanded = false })
                    DropdownMenuItem(text = { Text("pretrain_moe") }, onClick = { fromWeight = "pretrain_moe"; fromWeightExpanded = false })
                }
            }

            Text(
                text = "数据集配置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Box {
                OutlinedTextField(
                    value = dataset,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("数据集") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { datasetExpanded = true }) {
                            Icon(Icons.Filled.ArrowDropDown, null)
                        }
                    }
                )
                DropdownMenu(expanded = datasetExpanded, onDismissRequest = { datasetExpanded = false }) {
                    DropdownMenuItem(text = { Text("sft_t2t") }, onClick = { dataset = "sft_t2t"; datasetExpanded = false })
                    DropdownMenuItem(text = { Text("sft_t2t_mini") }, onClick = { dataset = "sft_t2t_mini"; datasetExpanded = false })
                }
            }

            Text(
                text = "训练参数",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
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
            OutlinedTextField(
                value = maxSeqLen,
                onValueChange = { maxSeqLen = it },
                label = { Text("最大序列长度") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            startResult?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (it.startsWith("成功")) Color(0xFF333333).copy(alpha = 0.1f)
                        else Color(0xFF888888).copy(alpha = 0.1f)
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
                            val config = SftConfig(
                                dataPath = dataset,
                                fromWeight = fromWeight,
                                learningRate = learningRate.toFloatOrNull() ?: 1e-5f,
                                batchSize = batchSize.toIntOrNull() ?: 16,
                                epochs = epochs.toIntOrNull() ?: 2,
                                maxSeqLen = maxSeqLen.toIntOrNull() ?: 768
                            )
                            val response = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                ApiClient.apiService.startSft(config)
                            }
                            startResult = "成功启动训练，任务ID: ${response.taskId}"
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
                    Text("开始训练")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
