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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minimind.app.network.ApiClient
import com.minimind.app.network.model.LoraConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoraConfigScreen(onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var fromWeight by remember { mutableStateOf("full_sft") }
    var fromWeightExpanded by remember { mutableStateOf(false) }
    var dataset by remember { mutableStateOf("lora_data") }
    var rank by remember { mutableStateOf("8") }
    var alpha by remember { mutableStateOf("16") }
    var learningRate by remember { mutableStateOf("1e-4") }
    var batchSize by remember { mutableStateOf("32") }
    var epochs by remember { mutableStateOf("10") }
    var loraName by remember { mutableStateOf("lora_custom") }
    var isStarting by remember { mutableStateOf(false) }
    var startResult by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LoRA 微调配置") },
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
                    text = "LoRA 是一种参数高效的微调方法。与全参数微调不同，LoRA 只训练模型中极少量新增的参数（通常不到总参数的 1%），就能让模型适应新的任务或领域。\n\n适用场景：当你想让模型在某个特定领域（如医学、法律、编程等）表现更好，或者想让模型学会特定的对话风格时，LoRA 是最合适的选择。它训练速度快、占用资源少，非常适合在手机上运行。\n\nRank 和 Alpha：这两个参数控制 LoRA 的\"学习容量\"。Rank 越大，LoRA 能学到的内容越复杂，但训练成本也越高。Alpha 控制学习强度。一般保持默认值即可。",
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
                            Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown, null)
                        }
                    }
                )
                DropdownMenu(expanded = fromWeightExpanded, onDismissRequest = { fromWeightExpanded = false }) {
                    DropdownMenuItem(text = { Text("full_sft") }, onClick = { fromWeight = "full_sft"; fromWeightExpanded = false })
                    DropdownMenuItem(text = { Text("pretrain") }, onClick = { fromWeight = "pretrain"; fromWeightExpanded = false })
                }
            }

            Text(
                text = "数据集配置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = dataset,
                onValueChange = { dataset = it },
                label = { Text("LoRA 数据集 (支持上传自定义 jsonl)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                text = "LoRA 参数",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = rank,
                onValueChange = { rank = it },
                label = { Text("Rank") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = alpha,
                onValueChange = { alpha = it },
                label = { Text("Alpha") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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

            Text(
                text = "LoRA 权重名称",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = loraName,
                onValueChange = { loraName = it },
                label = { Text("LoRA 权重名称") },
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
                            val config = LoraConfig(
                                dataPath = dataset,
                                fromWeight = fromWeight,
                                loraName = loraName,
                                epochs = epochs.toIntOrNull() ?: 10,
                                batchSize = batchSize.toIntOrNull() ?: 32,
                                learningRate = learningRate.toFloatOrNull() ?: 1e-4f,
                                rank = rank.toIntOrNull() ?: 8,
                                alpha = alpha.toIntOrNull() ?: 16
                            )
                            val response = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                ApiClient.apiService.startLora(config)
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
