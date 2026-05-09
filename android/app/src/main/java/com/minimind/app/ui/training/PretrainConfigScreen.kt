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
import androidx.compose.ui.unit.dp
import com.minimind.app.network.ApiClient
import com.minimind.app.network.model.PretrainConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PretrainConfigScreen(onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var dataset by remember { mutableStateOf("pretrain_t2t") }
    var datasetExpanded by remember { mutableStateOf(false) }
    var hiddenSize by remember { mutableStateOf("768") }
    var hiddenSizeExpanded by remember { mutableStateOf(false) }
    var numLayers by remember { mutableStateOf("8") }
    var numLayersExpanded by remember { mutableStateOf(false) }
    var useMoe by remember { mutableStateOf(false) }
    var learningRate by remember { mutableStateOf("5e-4") }
    var batchSize by remember { mutableStateOf("32") }
    var epochs by remember { mutableStateOf("2") }
    var maxSeqLen by remember { mutableStateOf("340") }
    var saveInterval by remember { mutableStateOf("1000") }
    var fromWeight by remember { mutableStateOf("none") }
    var fromWeightExpanded by remember { mutableStateOf(false) }
    var isStarting by remember { mutableStateOf(false) }
    var startResult by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预训练配置") },
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
                            Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown, null)
                        }
                    }
                )
                DropdownMenu(expanded = datasetExpanded, onDismissRequest = { datasetExpanded = false }) {
                    DropdownMenuItem(text = { Text("pretrain_t2t") }, onClick = { dataset = "pretrain_t2t"; datasetExpanded = false })
                    DropdownMenuItem(text = { Text("pretrain_t2t_mini") }, onClick = { dataset = "pretrain_t2t_mini"; datasetExpanded = false })
                }
            }

            Text(
                text = "模型配置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Box {
                OutlinedTextField(
                    value = hiddenSize,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("隐藏层维度") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { hiddenSizeExpanded = true }) {
                            Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown, null)
                        }
                    }
                )
                DropdownMenu(expanded = hiddenSizeExpanded, onDismissRequest = { hiddenSizeExpanded = false }) {
                    DropdownMenuItem(text = { Text("512") }, onClick = { hiddenSize = "512"; hiddenSizeExpanded = false })
                    DropdownMenuItem(text = { Text("768") }, onClick = { hiddenSize = "768"; hiddenSizeExpanded = false })
                }
            }
            Box {
                OutlinedTextField(
                    value = numLayers,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("层数") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { numLayersExpanded = true }) {
                            Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown, null)
                        }
                    }
                )
                DropdownMenu(expanded = numLayersExpanded, onDismissRequest = { numLayersExpanded = false }) {
                    DropdownMenuItem(text = { Text("8") }, onClick = { numLayers = "8"; numLayersExpanded = false })
                    DropdownMenuItem(text = { Text("16") }, onClick = { numLayers = "16"; numLayersExpanded = false })
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("MoE (混合专家)", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = useMoe, onCheckedChange = { useMoe = it })
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
            OutlinedTextField(
                value = saveInterval,
                onValueChange = { saveInterval = it },
                label = { Text("保存间隔") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                text = "基础权重",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Box {
                OutlinedTextField(
                    value = if (fromWeight == "none") "从头开始" else fromWeight,
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
                    DropdownMenuItem(text = { Text("从头开始") }, onClick = { fromWeight = "none"; fromWeightExpanded = false })
                    DropdownMenuItem(text = { Text("已有权重") }, onClick = { fromWeight = "pretrain"; fromWeightExpanded = false })
                }
            }

            startResult?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (it.startsWith("成功")) com.minimind.app.ui.theme.SuccessGreen.copy(alpha = 0.1f)
                        else com.minimind.app.ui.theme.ErrorRed.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isStarting = true
                        try {
                            val config = PretrainConfig(
                                dataPath = dataset,
                                hiddenSize = hiddenSize.toIntOrNull() ?: 768,
                                numHiddenLayers = numLayers.toIntOrNull() ?: 8,
                                useMoe = useMoe,
                                learningRate = learningRate.toFloatOrNull() ?: 5e-4f,
                                batchSize = batchSize.toIntOrNull() ?: 32,
                                epochs = epochs.toIntOrNull() ?: 2,
                                maxSeqLen = maxSeqLen.toIntOrNull() ?: 340,
                                saveInterval = saveInterval.toIntOrNull() ?: 1000,
                                fromWeight = fromWeight
                            )
                            val response = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                ApiClient.apiService.startPretrain(config)
                            }
                            startResult = "成功启动训练，任务ID: ${response.taskId}"
                        } catch (e: Exception) {
                            startResult = "启动失败: ${e.message}"
                        }
                        isStarting = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isStarting,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isStarting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("开始训练")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
