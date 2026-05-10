package com.minimind.app.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minimind.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    onNavigateToPretrain: () -> Unit,
    onNavigateToSft: () -> Unit,
    onNavigateToDistillation: () -> Unit,
    onNavigateToLora: () -> Unit,
    onNavigateToRl: () -> Unit,
    onNavigateToAgent: () -> Unit,
    onNavigateToMonitor: (String) -> Unit,
    viewModel: TrainingViewModel = viewModel()
) {
    val steps by viewModel.steps.collectAsState()
    val currentTaskStatus by viewModel.currentTaskStatus.collectAsState()
    val isTraining by viewModel.isTraining.collectAsState()

    val routeToNav = mapOf(
        "pretrain" to onNavigateToPretrain,
        "sft" to onNavigateToSft,
        "distillation" to onNavigateToDistillation,
        "lora" to onNavigateToLora,
        "rl" to onNavigateToRl,
        "agent" to onNavigateToAgent
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("训练") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "训练流程",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "按照以下步骤训练你的模型，必须步骤需要按顺序完成",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            steps.forEachIndexed { index, step ->
                StepCard(
                    step = step,
                    onClick = {
                        routeToNav[step.route]?.invoke()
                    },
                    onSkip = {
                        viewModel.skipStep(index)
                    }
                )
                if (index < steps.size - 1) {
                    StepConnector(
                        isCompleted = step.status == "completed",
                        isActive = step.status == "running"
                    )
                }
            }

            if (isTraining && currentTaskStatus != null) {
                Spacer(modifier = Modifier.height(16.dp))
                CurrentTrainingCard(
                    status = currentTaskStatus!!,
                    onNavigateToMonitor = { onNavigateToMonitor(currentTaskStatus!!.taskId) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StepCard(
    step: TrainingStep,
    onClick: () -> Unit,
    onSkip: () -> Unit
) {
    val isSkipped = step.status == "skipped"

    val statusColor = when (step.status) {
        "completed" -> Color(0xFF333333)
        "running" -> Color(0xFF333333)
        "failed" -> Color(0xFFBBBBBB)
        "skipped" -> Color(0xFFCCCCCC)
        else -> Color(0xFFCCCCCC)
    }

    val statusText = when (step.status) {
        "completed" -> "已完成"
        "running" -> "进行中"
        "failed" -> "失败"
        "skipped" -> "已跳过"
        else -> "未开始"
    }

    val statusIcon = when (step.status) {
        "completed" -> Icons.Default.CheckCircle
        "running" -> Icons.Default.Pending
        "failed" -> Icons.Default.Error
        "skipped" -> Icons.Default.SkipNext
        else -> Icons.Default.RadioButtonUnchecked
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (step.status == "running") {
                Color(0xFF333333).copy(alpha = 0.08f)
            } else if (isSkipped) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (step.status == "running") {
            CardDefaults.outlinedCardBorder()
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step.index.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = step.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (isSkipped) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isSkipped) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (step.required) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ErrorRed.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "必须",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = ErrorRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "可选",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSkipped) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textDecoration = if (isSkipped) TextDecoration.LineThrough else TextDecoration.None
                )
                if (!step.required && step.status == "not_started") {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onSkip,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "跳过",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun StepConnector(isCompleted: Boolean, isActive: Boolean) {
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(24.dp)
            .padding(start = 35.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(24.dp)
                .background(
                    if (isCompleted) Color(0xFF333333).copy(alpha = 0.5f)
                    else if (isActive) Color(0xFF333333).copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
        )
    }
}

@Composable
private fun CurrentTrainingCard(
    status: com.minimind.app.network.model.TrainingStatus,
    onNavigateToMonitor: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToMonitor),
        colors = CardDefaults.cardColors(containerColor = PrimaryVariant.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Pending,
                    contentDescription = null,
                    tint = PrimaryVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "当前训练任务",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "任务ID: ${status.taskId.take(8)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Epoch: ${status.epoch} | Step: ${status.step}/${status.totalSteps}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Loss: ${String.format("%.4f", status.loss)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = if (status.totalSteps > 0) status.step.toFloat() / status.totalSteps else 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PrimaryVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击查看详情",
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryVariant
            )
        }
    }
}
