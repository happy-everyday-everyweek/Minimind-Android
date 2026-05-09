package com.minimind.app.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimind.app.MiniMindApp
import com.minimind.app.data.ActivityRecord
import com.minimind.app.network.ApiClient
import com.minimind.app.network.model.TrainingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrainingStep(
    val index: Int,
    val name: String,
    val description: String,
    val route: String,
    val required: Boolean,
    val status: String = "not_started"
)

class TrainingViewModel : ViewModel() {

    private val activityRepository = MiniMindApp.instance.activityRepository

    private val _steps = MutableStateFlow<List<TrainingStep>>(listOf(
        TrainingStep(1, "预训练 (Pretrain)", "从大规模文本数据学习语言基础能力", "pretrain", true),
        TrainingStep(2, "有监督微调 (SFT)", "使用指令数据集进行有监督微调", "sft", true),
        TrainingStep(3, "黑盒知识蒸馏", "利用教师模型的知识进行蒸馏训练", "distillation", false),
        TrainingStep(4, "LoRA 微调", "使用 LoRA 方法进行参数高效微调", "lora", false),
        TrainingStep(5, "强化学习 (RL)", "使用 RLHF 方法对齐模型行为", "rl", false),
        TrainingStep(6, "Agentic RL", "训练模型使用工具和执行复杂任务", "agent", false)
    ))
    val steps: StateFlow<List<TrainingStep>> = _steps.asStateFlow()

    private val _currentTaskStatus = MutableStateFlow<TrainingStatus?>(null)
    val currentTaskStatus: StateFlow<TrainingStatus?> = _currentTaskStatus.asStateFlow()

    private val _isTraining = MutableStateFlow(false)
    val isTraining: StateFlow<Boolean> = _isTraining.asStateFlow()

    fun updateStepStatus(index: Int, status: String) {
        val current = _steps.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(status = status)
            _steps.value = current
        }
    }

    fun checkCurrentTraining() {
        viewModelScope.launch {
            try {
                val status = _currentTaskStatus.value
                if (status != null) {
                    val updated = withContext(Dispatchers.IO) {
                        ApiClient.apiService.getTrainingStatus(status.taskId)
                    }
                    _currentTaskStatus.value = updated
                    _isTraining.value = updated.status == "running"
                }
            } catch (e: Exception) {
                _isTraining.value = false
            }
        }
    }

    fun setCurrentTask(status: TrainingStatus) {
        _currentTaskStatus.value = status
        _isTraining.value = status.status == "running"
        activityRepository.addActivity(
            ActivityRecord(
                id = status.taskId,
                type = "training",
                title = "训练任务: ${status.taskId.take(8)}",
                status = status.status
            )
        )
    }
}
