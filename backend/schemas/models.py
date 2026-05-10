from pydantic import BaseModel, Field
from typing import Optional, List


class ModelInfo(BaseModel):
    id: str
    name: str
    weight_name: str
    hidden_size: int
    num_hidden_layers: int
    use_moe: bool
    description: str = ""
    file_path: Optional[str] = None
    file_size: Optional[int] = None
    is_loaded: bool = False


class ModelListResponse(BaseModel):
    models: List[ModelInfo]


class ModelDownloadRequest(BaseModel):
    model_id: str


class ModelLoadRequest(BaseModel):
    weight_name: str = "full_sft"
    hidden_size: int = 768
    num_hidden_layers: int = 8
    use_moe: bool = False
    lora_weight: Optional[str] = None
    max_seq_len: int = 8192
    inference_rope_scaling: bool = False


class ModelExportRequest(BaseModel):
    export_path: str
