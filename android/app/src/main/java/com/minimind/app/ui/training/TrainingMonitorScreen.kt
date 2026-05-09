package com.minimind.app.ui.training

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minimind.app.network.ApiClient
import com.minimind.app.network.WebSocketClient
import com.minimind.app.network.WebSocketCallback
import com.minimind.app.network.model.TrainingStatus
import com.minimind.app.ui.theme.Primary
import com.minimind.app.ui.theme.SuccessGreen
import com.minimind.app.ui.theme.ErrorRed
import com.minimind.app.ui.theme.WarningOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingMonitorScreen(
    taskId: String,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var trainingStatus by remember { mutableStateOf<TrainingStatus?>(null) }
    var lossHistory by remember { mutableStateOf<List<Pair<Int, Float>>>(emptyList()) }
    var logs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isPaused by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val webSocketClient = remember { WebSocketClient() }

    LaunchedEffect(taskId) {
        webSocketClient.setCallback(object : WebSocketCallback {
            override fun onConnected() {}
            override fun onMessage(text: String) {
                logs = logs + text
            }
            override fun onToken(token: com.minimind.app.network.model.StreamToken) {}
            override fun onTrainingStatus(status: TrainingStatus) {
                trainingStatus = status
                isLoading = false
                lossHistory = lossHistory + (status.step to status.loss)
                if (status.log != null) {
                    logs = logs + status.log
                }
            }
            override fun onError(error: Throwable) {
                isLoading = false
            }
            override fun onDisconnected() {}
        })
        webSocketClient.connectTraining(ApiClient.getBaseUrl(), taskId)

        try {
            val status = kotlinx.coroutines.withContext(Dispatchers.IO) {
                ApiClient.apiService.getTrainingStatus(taskId)
            }
            trainingStatus = status
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { webSocketClient.disconnect() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("训练监控") },
                navigationIcon = {
                    IconButton(onClick = {
                        webSocketClient.disconnect()
                        onBack()
                    }) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            trainingStatus?.let { status ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "任务信息",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("任务ID: ${status.taskId.take(12)}", style = MaterialTheme.typography.bodyMedium)
                        Text("状态: ${when (status.status) {
                            "running" -> "运行中"
                            "paused" -> "已暂停"
                            "completed" -> "已完成"
                            "failed" -> "失败"
                            else -> status.status
                        }}", style = MaterialTheme.typography.bodyMedium)
                        Text("Epoch: ${status.epoch} | Step: ${status.step}/${status.totalSteps}",
                            style = MaterialTheme.typography.bodyMedium)
                        Text("Loss: ${String.format("%.4f", status.loss)}",
                            style = MaterialTheme.typography.bodyMedium)
                        status.reward?.let {
                            Text("Reward: ${String.format("%.4f", it)}",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (status.totalSteps > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("训练进度", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = status.step.toFloat() / status.totalSteps,
                            modifier = Modifier.weight(1f).height(8.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = Primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${(status.step.toFloat() / status.totalSteps * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (lossHistory.size >= 2) {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Loss 曲线",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LossChart(
                                data = lossHistory,
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (status.status == "running") {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        kotlinx.coroutines.withContext(Dispatchers.IO) {
                                            ApiClient.apiService.pauseTraining(taskId)
                                        }
                                        isPaused = true
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("暂停")
                        }
                    } else if (status.status == "paused") {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        kotlinx.coroutines.withContext(Dispatchers.IO) {
                                            ApiClient.apiService.resumeTraining(taskId)
                                        }
                                        isPaused = false
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("恢复")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                                        ApiClient.apiService.stopTraining(taskId)
                                    }
                                    webSocketClient.disconnect()
                                    onBack()
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("停止")
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Text(
                    text = "训练日志",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState
                ) {
                    if (logs.isEmpty()) {
                        item {
                            Text(
                                "暂无日志",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(logs) { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LossChart(
    data: List<Pair<Int, Float>>,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) return

    val minLoss = data.minOf { it.second }
    val maxLoss = data.maxOf { it.second }
    val lossRange = maxLoss - minLoss
    val safeRange = if (lossRange == 0f) 1f else lossRange

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val padding = 40f

        val chartWidth = canvasWidth - padding * 2
        val chartHeight = canvasHeight - padding * 2

        val stepRange = data.last().first - data.first().first
        val safeStepRange = if (stepRange == 0) 1 else stepRange

        val paint = android.graphics.Paint().apply {
            textSize = 24f
            color = android.graphics.Color.GRAY
            textAlign = android.graphics.Paint.Align.RIGHT
        }

        drawContext.canvas.nativeCanvas.drawText(
            String.format("%.2f", maxLoss),
            padding - 4f,
            padding + 8f,
            paint
        )
        drawContext.canvas.nativeCanvas.drawText(
            String.format("%.2f", minLoss),
            padding - 4f,
            canvasHeight - padding + 8f,
            paint
        )

        val path = Path()
        data.forEachIndexed { index, (step, loss) ->
            val x = padding + (step - data.first().first).toFloat() / safeStepRange * chartWidth
            val y = padding + (1f - (loss - minLoss) / safeRange) * chartHeight
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = Primary, style = Stroke(width = 3f))

        data.lastOrNull()?.let { (_, loss) ->
            val x = padding + chartWidth
            val y = padding + (1f - (loss - minLoss) / safeRange) * chartHeight
            drawCircle(color = Primary, radius = 5f, center = Offset(x, y))
        }
    }
}
