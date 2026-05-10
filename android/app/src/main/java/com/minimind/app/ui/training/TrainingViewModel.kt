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
        TrainingStep(1, "预训练 (Pretrain)",
            "让模型从大量文本中学习语言的基础规律。就像让一个人先广泛阅读各种书籍，积累基础的语言能力和常识知识。这是训练任何语言模型的第一步，模型会学会理解和生成基本的文字内容。",
            "pretrain", true),
        TrainingStep(2, "有监督微调 (SFT)",
            "在预训练的基础上，用带有正确答案的对话数据来教模型如何回答问题。就像给学生做有标准答案的练习题，让模型学会按照指令回答、进行多轮对话、遵循特定格式等。",
            "sft", true),
        TrainingStep(3, "知识蒸馏",
            "借助一个更强大的模型（教师模型）来帮助训练你的模型（学生模型）。教师模型会对你提供的问题给出高质量的回答，然后你的模型通过学习这些回答来提升自己的能力。就像让名师为你的学生批改作业并提供示范答案。你可以通过配置外部大语言模型的 API 来接入教师模型。",
            "distillation", false),
        TrainingStep(4, "LoRA 微调",
            "一种高效的微调方法，只训练模型中很少一部分参数，就能让模型适应特定领域或任务。比如你的模型在医学知识上不够好，可以用医学数据做 LoRA 微调，用很小的代价就能获得更好的领域表现。相比全参数微调，LoRA 速度快、占用资源少，适合在手机上运行。",
            "lora", false),
        TrainingStep(5, "强化学习 (RL)",
            "通过奖励和惩罚的机制来进一步优化模型的行为。就像训练宠物一样，做得好给奖励，做得不好就减少奖励。这里提供了三种算法：DPO（直接偏好优化，用人类偏好数据训练）、PPO（近端策略优化，经典强化学习算法）、GRPO（分组相对策略优化，更稳定的强化学习方法）。",
            "rl", false),
        TrainingStep(6, "Agentic RL",
            "训练模型学会使用外部工具来完成复杂任务。比如让模型学会查询天气、执行数学计算、搜索信息等。你可以自定义工具和奖励规则，让模型在多轮交互中学会何时调用工具、如何正确使用工具。",
            "agent", false)
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

    fun skipStep(index: Int) {
        val current = _steps.value.toMutableList()
        if (index in current.indices && !current[index].required) {
            current[index] = current[index].copy(status = "skipped")
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
