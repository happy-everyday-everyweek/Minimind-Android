from pydantic import BaseModel
from typing import Optional, List, Dict, Any


class DatasetInfo(BaseModel):
    id: str
    name: str
    file_path: str
    file_size: int = 0
    num_lines: int = 0
    description: str = ""


class DatasetListResponse(BaseModel):
    datasets: List[DatasetInfo]


class DatasetPreviewResponse(BaseModel):
    id: str
    lines: List[Dict[str, Any]]
    total_lines: int
