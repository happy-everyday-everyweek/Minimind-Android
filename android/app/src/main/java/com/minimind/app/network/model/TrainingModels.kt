package com.minimind.app.network.model

import com.google.gson.annotations.SerializedName

data class PretrainConfig(
    @SerializedName("data_path") val dataPath: String = "pretrain_t2t",
    @SerializedName("hidden_size") val hiddenSize: Int = 768,
    @SerializedName("num_hidden_layers") val numHiddenLayers: Int = 8,
    @SerializedName("use_moe") val useMoe: Boolean = false,
    val epochs: Int = 2,
    @SerializedName("batch_size") val batchSize: Int = 32,
    @SerializedName("learning_rate") val learningRate: Float = 5e-4f,
    @SerializedName("max_seq_len") val maxSeqLen: Int = 340,
    @SerializedName("save_interval") val saveInterval: Int = 1000,
    @SerializedName("from_weight") val fromWeight: String = "none"
)

data class SftConfig(
    @SerializedName("data_path") val dataPath: String = "sft_t2t",
    @SerializedName("from_weight") val fromWeight: String = "pretrain",
    val epochs: Int = 2,
    @SerializedName("batch_size") val batchSize: Int = 16,
    @SerializedName("learning_rate") val learningRate: Float = 1e-5f,
    @SerializedName("max_seq_len") val maxSeqLen: Int = 768
)

data class LoraConfig(
    @SerializedName("data_path") val dataPath: String = "lora_data",
    @SerializedName("from_weight") val fromWeight: String = "full_sft",
    @SerializedName("lora_name") val loraName: String = "lora_custom",
    val epochs: Int = 10,
    @SerializedName("batch_size") val batchSize: Int = 32,
    @SerializedName("learning_rate") val learningRate: Float = 1e-4f,
    val rank: Int = 8,
    val alpha: Int = 16
)

data class DpoConfig(
    @SerializedName("data_path") val dataPath: String = "dpo_data",
    @SerializedName("from_weight") val fromWeight: String = "full_sft",
    val beta: Float = 0.15f,
    val epochs: Int = 1,
    @SerializedName("batch_size") val batchSize: Int = 4,
    @SerializedName("learning_rate") val learningRate: Float = 4e-8f
)

data class PpoConfig(
    @SerializedName("data_path") val dataPath: String = "rl_data",
    @SerializedName("from_weight") val fromWeight: String = "full_sft",
    @SerializedName("clip_epsilon") val clipEpsilon: Float = 0.2f,
    @SerializedName("kl_coef") val klCoef: Float = 0.02f,
    val epochs: Int = 1,
    @SerializedName("batch_size") val batchSize: Int = 2,
    @SerializedName("learning_rate") val learningRate: Float = 3e-7f
)

data class GrpoConfig(
    @SerializedName("data_path") val dataPath: String = "rl_data",
    @SerializedName("from_weight") val fromWeight: String = "full_sft",
    @SerializedName("loss_type") val lossType: String = "cispo",
    val beta: Float = 0.1f,
    val epsilon: Float = 0.2f,
    @SerializedName("num_generations") val numGenerations: Int = 6,
    val epochs: Int = 1,
    @SerializedName("batch_size") val batchSize: Int = 2,
    @SerializedName("learning_rate") val learningRate: Float = 3e-7f
)

data class AgentConfig(
    @SerializedName("data_path") val dataPath: String = "agent_data",
    @SerializedName("from_weight") val fromWeight: String = "full_sft",
    @SerializedName("loss_type") val lossType: String = "cispo",
    val beta: Float = 0.1f,
    @SerializedName("num_generations") val numGenerations: Int = 4,
    val epochs: Int = 1,
    @SerializedName("batch_size") val batchSize: Int = 2,
    @SerializedName("learning_rate") val learningRate: Float = 3e-7f,
    @SerializedName("tools_config") val toolsConfig: String = "{}",
    @SerializedName("reward_config") val rewardConfig: String = "{}"
)

data class DistillationConfig(
    @SerializedName("teacher_api_base") val teacherApiBase: String,
    @SerializedName("teacher_api_key") val teacherApiKey: String,
    @SerializedName("teacher_model_name") val teacherModelName: String,
    @SerializedName("student_weight") val studentWeight: String,
    @SerializedName("data_path") val dataPath: String = "distill_data",
    val epochs: Int = 2,
    @SerializedName("batch_size") val batchSize: Int = 16,
    @SerializedName("learning_rate") val learningRate: Float = 1e-5f
)

data class TrainingStatus(
    @SerializedName("task_id") val taskId: String,
    val status: String,
    val epoch: Int,
    val step: Int,
    @SerializedName("total_steps") val totalSteps: Int,
    val loss: Float,
    val reward: Float? = null,
    val log: String? = null
)

data class TrainingStartResponse(
    @SerializedName("task_id") val taskId: String
)

data class ResourceLimitsRequest(
    @SerializedName("max_cpu_percent") val max_cpu_percent: Int = 80,
    @SerializedName("max_memory_mb") val max_memory_mb: Int = 2048,
    @SerializedName("max_training_processes") val max_training_processes: Int = 1
)

data class ResourceLimitsResponse(
    @SerializedName("max_cpu_percent") val max_cpu_percent: Int = 80,
    @SerializedName("max_memory_mb") val max_memory_mb: Int = 2048,
    @SerializedName("max_training_processes") val max_training_processes: Int = 1
)
