package com.minimind.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minimind.app.MiniMindApp
import com.minimind.app.backend.BackendManager
import com.minimind.app.data.ActivityRecord
import com.minimind.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToInference: () -> Unit,
    onNavigateToTraining: () -> Unit,
    onNavigateToModels: () -> Unit,
    onNavigateToDatasets: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val backendStatus by viewModel.backendStatus.collectAsState()
    val recentActivities by viewModel.recentActivities.collectAsState()
    val isInitializing by viewModel.isInitializing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MiniMind") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                    IconButton(onClick = onNavigateToModels) {
                        Icon(Icons.Default.Storage, contentDescription = "模型")
                    }
                    IconButton(onClick = onNavigateToDatasets) {
                        Icon(Icons.Default.Dataset, contentDescription = "数据集")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                AppHeader()
            }

            item {
                BackendStatusCard(
                    status = backendStatus,
                    isInitializing = isInitializing,
                    onReinitialize = { viewModel.reinitialize() }
                )
            }

            item {
                QuickStartSection(
                    onNavigateToInference = onNavigateToInference,
                    onNavigateToTraining = onNavigateToTraining
                )
            }

            item {
                RecentActivitiesSection(
                    activities = recentActivities,
                    onNavigateToModels = onNavigateToModels,
                    onNavigateToDatasets = onNavigateToDatasets
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Primary
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = androidx.compose.ui.graphics.Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "MiniMind",
                style = MaterialTheme.typography.headlineLarge,
                color = androidx.compose.ui.graphics.Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "轻量级大语言模型训练与推理平台",
                style = MaterialTheme.typography.bodyLarge,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun BackendStatusCard(
    status: BackendManager.BackendStatus,
    isInitializing: Boolean,
    onReinitialize: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        when (status) {
                            BackendManager.BackendStatus.ONLINE -> SuccessGreen
                            BackendManager.BackendStatus.OFFLINE -> ErrorRed
                            BackendManager.BackendStatus.ERROR -> ErrorRed
                            BackendManager.BackendStatus.CHECKING -> WarningOrange
                            BackendManager.BackendStatus.UNKNOWN -> WarningOrange
                        }
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = when (status) {
                    BackendManager.BackendStatus.ONLINE -> "后端服务在线"
                    BackendManager.BackendStatus.OFFLINE -> "后端服务离线"
                    BackendManager.BackendStatus.ERROR -> "后端服务异常"
                    BackendManager.BackendStatus.CHECKING -> "正在检查服务状态..."
                    BackendManager.BackendStatus.UNKNOWN -> "服务状态未知"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            if (status != BackendManager.BackendStatus.ONLINE) {
                if (isInitializing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    TextButton(onClick = onReinitialize) {
                        Text("重新初始化")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStartSection(
    onNavigateToInference: () -> Unit,
    onNavigateToTraining: () -> Unit
) {
    Text(
        text = "快速开始",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStartCard(
            modifier = Modifier.weight(1f),
            title = "开始对话",
            description = "与模型进行推理对话",
            icon = Icons.Default.Chat,
            containerColor = Primary,
            onClick = onNavigateToInference
        )
        QuickStartCard(
            modifier = Modifier.weight(1f),
            title = "开始训练",
            description = "训练自定义模型",
            icon = Icons.Default.School,
            containerColor = Secondary,
            contentColor = Primary,
            onClick = onNavigateToTraining
        )
    }
}

@Composable
private fun QuickStartCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun RecentActivitiesSection(
    activities: List<ActivityRecord>,
    onNavigateToModels: () -> Unit,
    onNavigateToDatasets: () -> Unit
) {
    Text(
        text = "最近活动",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))
    if (activities.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "暂无活动记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    } else {
        activities.forEach { activity ->
            ActivityCard(activity)
        }
    }
}

@Composable
private fun ActivityCard(activity: ActivityRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (activity.type) {
                    "inference" -> Icons.Default.Chat
                    "training" -> Icons.Default.School
                    "download" -> Icons.Default.Download
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = when (activity.status) {
                    "completed" -> SuccessGreen
                    "running" -> Primary
                    "failed" -> ErrorRed
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatTimestamp(activity.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = when (activity.status) {
                    "completed" -> SuccessGreen.copy(alpha = 0.1f)
                    "running" -> Primary.copy(alpha = 0.1f)
                    "failed" -> ErrorRed.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = when (activity.status) {
                        "completed" -> "已完成"
                        "running" -> "进行中"
                        "failed" -> "失败"
                        else -> activity.status
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (activity.status) {
                        "completed" -> SuccessGreen
                        "running" -> Primary
                        "failed" -> ErrorRed
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
