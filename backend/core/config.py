import os

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
MINIMIND_ROOT = PROJECT_ROOT
TRAINER_DIR = os.path.join(MINIMIND_ROOT, 'trainer')
MODEL_DIR = os.path.join(MINIMIND_ROOT, 'model')
DATASET_DIR = os.path.join(MINIMIND_ROOT, 'dataset')
OUT_DIR = os.path.join(MINIMIND_ROOT, 'out')
CHECKPOINTS_DIR = os.path.join(MINIMIND_ROOT, 'checkpoints')
UPLOAD_DIR = os.path.join(os.path.dirname(__file__), '..', 'uploads')
LLM_API_CONFIG_PATH = os.path.join(os.path.dirname(__file__), '..', 'llm_api_config.json')
RESOURCE_LIMITS_CONFIG_PATH = os.path.join(os.path.dirname(__file__), '..', 'resource_limits_config.json')

DEFAULT_RESOURCE_LIMITS = {
    "max_cpu_percent": 80,
    "max_memory_mb": 2048,
    "max_training_processes": 1
}

DEFAULT_DEVICE = "cpu"
DEFAULT_DTYPE = "float32"
DEFAULT_HIDDEN_SIZE = 768
DEFAULT_NUM_HIDDEN_LAYERS = 8
DEFAULT_MAX_SEQ_LEN = 8192
DEFAULT_USE_MOE = False

TRAINING_SCRIPTS = {
    "pretrain": os.path.join(TRAINER_DIR, "train_pretrain.py"),
    "sft": os.path.join(TRAINER_DIR, "train_full_sft.py"),
    "lora": os.path.join(TRAINER_DIR, "train_lora.py"),
    "dpo": os.path.join(TRAINER_DIR, "train_dpo.py"),
    "ppo": os.path.join(TRAINER_DIR, "train_ppo.py"),
    "grpo": os.path.join(TRAINER_DIR, "train_grpo.py"),
    "agent": os.path.join(TRAINER_DIR, "train_agent.py"),
    "distillation": os.path.join(TRAINER_DIR, "train_distillation.py"),
}

PRESET_MODELS = [
    {
        "id": "pretrain_768",
        "name": "MiniMind Pretrain (768)",
        "weight_name": "pretrain",
        "hidden_size": 768,
        "num_hidden_layers": 8,
        "use_moe": False,
        "description": "预训练模型权重",
    },
    {
        "id": "full_sft_768",
        "name": "MiniMind Full SFT (768)",
        "weight_name": "full_sft",
        "hidden_size": 768,
        "num_hidden_layers": 8,
        "use_moe": False,
        "description": "SFT微调模型权重",
    },
    {
        "id": "pretrain_768_moe",
        "name": "MiniMind Pretrain MoE (768)",
        "weight_name": "pretrain",
        "hidden_size": 768,
        "num_hidden_layers": 8,
        "use_moe": True,
        "description": "预训练MoE模型权重",
    },
    {
        "id": "full_sft_768_moe",
        "name": "MiniMind Full SFT MoE (768)",
        "weight_name": "full_sft",
        "hidden_size": 768,
        "num_hidden_layers": 8,
        "use_moe": True,
        "description": "SFT微调MoE模型权重",
    },
]
