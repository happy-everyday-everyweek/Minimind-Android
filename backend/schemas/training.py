from pydantic import BaseModel, Field
from typing import Optional


class PretrainRequest(BaseModel):
    data_path: str = "../dataset/pretrain_t2t_mini.jsonl"
    hidden_size: int = 768
    num_hidden_layers: int = 8
    use_moe: int = 0
    epochs: int = 2
    batch_size: int = 32
    learning_rate: float = 5e-4
    max_seq_len: int = 340
    save_interval: int = 1000
    from_weight: str = "none"
    device: str = "cpu"


class SFTRequest(BaseModel):
    data_path: str = "../dataset/sft_t2t_mini.jsonl"
    hidden_size: int = 768
    num_hidden_layers: int = 8
    use_moe: int = 0
    from_weight: str = "pretrain"
    epochs: int = 2
    batch_size: int = 16
    learning_rate: float = 1e-5
    max_seq_len: int = 768
    save_interval: int = 1000
    device: str = "cpu"


class LoRARequest(BaseModel):
    data_path: str = "../dataset/lora_medical.jsonl"
    hidden_size: int = 768
    num_hidden_layers: int = 8
    use_moe: int = 0
    from_weight: str = "full_sft"
    lora_name: str = "lora_medical"
    epochs: int = 10
    batch_size: int = 32
    learning_rate: float = 1e-4
    max_seq_len: int = 340
    save_interval: int = 1000
    device: str = "cpu"


class DPORequest(BaseModel):
    data_path: str = "../dataset/dpo.jsonl"
    hidden_size: int = 768
    num_hidden_layers: int = 8
    use_moe: int = 0
    from_weight: str = "full_sft"
    beta: float = 0.15
    epochs: int = 1
    batch_size: int = 4
    learning_rate: float = 4e-8
    max_seq_len: int = 1024
    save_interval: int = 100
    device: str = "cpu"


class PPORequest(BaseModel):
    data_path: str = "../dataset/rlaif.jsonl"
    hidden_size: int = 768
    num_hidden_layers: int = 8
    use_moe: int = 0
    from_weight: str = "full_sft"
    clip_epsilon: float = 0.2
    kl_coef: float = 0.02
    epochs: int = 1
    batch_size: int = 2
    learning_rate: float = 3e-7
    max_seq_len: int = 768
    max_gen_len: int = 1024
    save_interval: int = 10
    device: str = "cpu"


class GRPORequest(BaseModel):
    data_path: str = "../dataset/rlaif.jsonl"
    hidden_size: int = 768
    num_hidden_layers: int = 8
    use_moe: int = 0
    from_weight: str = "full_sft"
    loss_type: str = "cispo"
    beta: float = 0.1
    epsilon: float = 0.2
    num_generations: int = 6
    epochs: int = 1
    batch_size: int = 2
    learning_rate: float = 3e-7
    max_seq_len: int = 768
    max_gen_len: int = 1024
    save_interval: int = 10
    device: str = "cpu"


class AgentRLRequest(BaseModel):
    data_path: str = "../dataset/agent_rl.jsonl"
    hidden_size: int = 768
    num_hidden_layers: int = 8
    use_moe: int = 0
    from_weight: str = "full_sft"
    loss_type: str = "cispo"
    beta: float = 0.1
    num_generations: int = 4
    max_gen_len: int = 768
    epochs: int = 1
    batch_size: int = 2
    learning_rate: float = 3e-7
    max_seq_len: int = 1024
    save_interval: int = 10
    device: str = "cpu"
    tools_config: Optional[str] = None
    reward_config: Optional[str] = None


class DistillationRequest(BaseModel):
    data_path: str = "../dataset/sft_t2t_mini.jsonl"
    student_hidden_size: int = 768
    student_num_layers: int = 8
    student_use_moe: int = 0
    teacher_hidden_size: int = 768
    teacher_num_layers: int = 8
    teacher_use_moe: int = 1
    from_student_weight: str = "full_sft"
    from_teacher_weight: str = "full_sft"
    alpha: float = 0.5
    temperature: float = 1.5
    epochs: int = 6
    batch_size: int = 32
    learning_rate: float = 5e-6
    max_seq_len: int = 340
    save_interval: int = 100
    device: str = "cpu"


class TrainingStatusResponse(BaseModel):
    task_id: str
    task_type: str
    status: str
    progress: Optional[dict] = None
    message: Optional[str] = None


class TrainingLogsResponse(BaseModel):
    task_id: str
    logs: list
