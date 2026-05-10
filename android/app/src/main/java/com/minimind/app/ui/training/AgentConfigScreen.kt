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
import com.minimind.app.network.model.AgentConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentConfigScreen(
    onBack: () -> Unit,
    onNavigateToEnvEditor: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var fromWeight by remember { mutableStateOf("full_sft") }
    var fromWeightExpanded by remember { mutableStateOf(false) }
    var lossType by remember { mutableStateOf("cispo") }
    var lossTypeExpanded by remember { mutableStateOf(false) }
    var beta by remember { mutableStateOf("0.1") }
    var numGenerations by remember { mutableStateOf("4") }
    var learningRate by remember { mutableStateOf("3e-7") }
    var batchSize by remember { mutableStateOf("2") }
    var epochs by remember { mutableStateOf("1") }
    var isStarting by remember { mutableStateOf(false) }
    var startResult by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agentic RL 配置") },
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
                    text = "Agentic RL 训练模型学会使用外部工具来完成复杂任务。训练后的模型能够在对话中判断何时需要调用工具、调用哪个工具、如何使用工具返回的结果。\n\n你可以自定义工具列表（如查询天气、数学计算等）和奖励规则（如工具调用是否正确、结果是否匹配预期等），模型会通过强化学习逐渐掌握工具使用的技巧。",
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
                text = "算法配置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Box {
                OutlinedTextField(
                    value = lossType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("算法") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { lossTypeExpanded = true }) {
                            Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown, null)
                        }
                    }
                )
                DropdownMenu(expanded = lossTypeExpanded, onDismissRequest = { lossTypeExpanded = false }) {
                    DropdownMenuItem(text = { Text("GRPO") }, onClick = { lossType = "grpo"; lossTypeExpanded = false })
                    DropdownMenuItem(text = { Text("CISPO") }, onClick = { lossType = "cispo"; lossTypeExpanded = false })
                }
            }

            Text(
                text = "训练参数",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = beta,
                onValueChange = { beta = it },
                label = { Text("Beta") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = numGenerations,
                onValueChange = { numGenerations = it },
                label = { Text("Num Generations") },
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

            OutlinedButton(
                onClick = onNavigateToEnvEditor,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("编辑环境")
            }

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
                            val config = AgentConfig(
                                fromWeight = fromWeight,
                                lossType = lossType,
                                beta = beta.toFloatOrNull() ?: 0.1f,
                                numGenerations = numGenerations.toIntOrNull() ?: 4,
                                learningRate = learningRate.toFloatOrNull() ?: 3e-7f,
                                batchSize = batchSize.toIntOrNull() ?: 2,
                                epochs = epochs.toIntOrNull() ?: 1
                            )
                            val response = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                ApiClient.apiService.startAgent(config)
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
