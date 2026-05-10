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
import com.minimind.app.network.model.DpoConfig
import com.minimind.app.network.model.PpoConfig
import com.minimind.app.network.model.GrpoConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RlConfigScreen(onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedAlgorithm by remember { mutableStateOf(0) }
    val algorithms = listOf("DPO", "PPO", "GRPO")

    var fromWeight by remember { mutableStateOf("full_sft") }
    var fromWeightExpanded by remember { mutableStateOf(false) }
    var dataset by remember { mutableStateOf("rl_data") }
    var isStarting by remember { mutableStateOf(false) }
    var startResult by remember { mutableStateOf<String?>(null) }

    var dpoBeta by remember { mutableStateOf("0.15") }
    var dpoLearningRate by remember { mutableStateOf("4e-8") }

    var ppoClipEpsilon by remember { mutableStateOf("0.2") }
    var ppoKlCoef by remember { mutableStateOf("0.02") }
    var ppoLearningRate by remember { mutableStateOf("3e-7") }

    var grpoLossType by remember { mutableStateOf("cispo") }
    var grpoLossTypeExpanded by remember { mutableStateOf(false) }
    var grpoBeta by remember { mutableStateOf("0.1") }
    var grpoEpsilon by remember { mutableStateOf("0.2") }
    var grpoNumGenerations by remember { mutableStateOf("6") }
    var grpoLearningRate by remember { mutableStateOf("3e-7") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("强化学习配置") },
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
                    text = "强化学习通过\"奖励\"机制来优化模型行为。模型会尝试生成不同的回答，表现好的获得正奖励，表现差的获得负奖励，从而逐渐学会更好的回答方式。\n\n三种算法说明：\n- DPO（直接偏好优化）：使用人类标注的偏好数据（好回答 vs 差回答）来训练，实现简单，适合入门。\n- PPO（近端策略优化）：经典的强化学习算法，通过奖励模型对回答打分来训练，效果更灵活但需要更多计算资源。\n- GRPO（分组相对策略优化）：对同一问题生成多个回答，通过组内比较来训练，训练更稳定，是目前主流的强化学习方法。",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Text(
                text = "算法选择",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                algorithms.forEachIndexed { index, algo ->
                    FilterChip(
                        selected = selectedAlgorithm == index,
                        onClick = { selectedAlgorithm = index },
                        label = { Text(algo) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text(
                text = "基础配置",
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
                    DropdownMenuItem(text = { Text("full_sft") }, onClick = { fromWeight = "full_sft"; fromWeightExpanded = false })
                    DropdownMenuItem(text = { Text("pretrain") }, onClick = { fromWeight = "pretrain"; fromWeightExpanded = false })
                }
            }
            OutlinedTextField(
                value = dataset,
                onValueChange = { dataset = it },
                label = { Text(if (selectedAlgorithm == 0) "偏好数据集" else "RL 数据集") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            when (selectedAlgorithm) {
                0 -> {
                    Text("DPO 参数", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(value = dpoBeta, onValueChange = { dpoBeta = it }, label = { Text("Beta") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = dpoLearningRate, onValueChange = { dpoLearningRate = it }, label = { Text("学习率") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                1 -> {
                    Text("PPO 参数", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(value = ppoClipEpsilon, onValueChange = { ppoClipEpsilon = it }, label = { Text("Clip Epsilon") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = ppoKlCoef, onValueChange = { ppoKlCoef = it }, label = { Text("KL Coef") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = ppoLearningRate, onValueChange = { ppoLearningRate = it }, label = { Text("学习率") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                2 -> {
                    Text("GRPO 参数", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Box {
                        OutlinedTextField(
                            value = grpoLossType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Loss Type") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { grpoLossTypeExpanded = true }) {
                                    Icon(Icons.Filled.ArrowDropDown, null)
                                }
                            }
                        )
                        DropdownMenu(expanded = grpoLossTypeExpanded, onDismissRequest = { grpoLossTypeExpanded = false }) {
                            DropdownMenuItem(text = { Text("grpo") }, onClick = { grpoLossType = "grpo"; grpoLossTypeExpanded = false })
                            DropdownMenuItem(text = { Text("cispo") }, onClick = { grpoLossType = "cispo"; grpoLossTypeExpanded = false })
                        }
                    }
                    OutlinedTextField(value = grpoBeta, onValueChange = { grpoBeta = it }, label = { Text("Beta") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = grpoEpsilon, onValueChange = { grpoEpsilon = it }, label = { Text("Epsilon") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = grpoNumGenerations, onValueChange = { grpoNumGenerations = it }, label = { Text("Num Generations") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = grpoLearningRate, onValueChange = { grpoLearningRate = it }, label = { Text("学习率") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
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
                            val response = when (selectedAlgorithm) {
                                0 -> kotlinx.coroutines.withContext(Dispatchers.IO) {
                                    ApiClient.apiService.startDpo(DpoConfig(
                                        dataPath = dataset, fromWeight = fromWeight,
                                        beta = dpoBeta.toFloatOrNull() ?: 0.15f,
                                        learningRate = dpoLearningRate.toFloatOrNull() ?: 4e-8f
                                    ))
                                }
                                1 -> kotlinx.coroutines.withContext(Dispatchers.IO) {
                                    ApiClient.apiService.startPpo(PpoConfig(
                                        dataPath = dataset, fromWeight = fromWeight,
                                        clipEpsilon = ppoClipEpsilon.toFloatOrNull() ?: 0.2f,
                                        klCoef = ppoKlCoef.toFloatOrNull() ?: 0.02f,
                                        learningRate = ppoLearningRate.toFloatOrNull() ?: 3e-7f
                                    ))
                                }
                                else -> kotlinx.coroutines.withContext(Dispatchers.IO) {
                                    ApiClient.apiService.startGrpo(GrpoConfig(
                                        dataPath = dataset, fromWeight = fromWeight,
                                        lossType = grpoLossType,
                                        beta = grpoBeta.toFloatOrNull() ?: 0.1f,
                                        epsilon = grpoEpsilon.toFloatOrNull() ?: 0.2f,
                                        numGenerations = grpoNumGenerations.toIntOrNull() ?: 6,
                                        learningRate = grpoLearningRate.toFloatOrNull() ?: 3e-7f
                                    ))
                                }
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
