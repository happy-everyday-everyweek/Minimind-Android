package com.minimind.app.navigation

object Routes {
    const val HOME = "home"
    const val INFERENCE = "inference"
    const val TRAINING = "training"
    const val PRETRAIN_CONFIG = "pretrain_config"
    const val SFT_CONFIG = "sft_config"
    const val DISTILLATION_CONFIG = "distillation_config"
    const val LORA_CONFIG = "lora_config"
    const val RL_CONFIG = "rl_config"
    const val AGENT_CONFIG = "agent_config"
    const val TRAINING_MONITOR = "training_monitor/{taskId}"
    const val AGENT_ENV_EDITOR = "agent_env_editor"
    const val MODELS = "models"
    const val DATASETS = "datasets"
    const val SETTINGS = "settings"

    fun trainingMonitor(taskId: String) = "training_monitor/$taskId"
}
