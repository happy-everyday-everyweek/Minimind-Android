from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any


class ChatMessage(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    messages: List[ChatMessage]
    temperature: float = Field(default=0.85, ge=0.0, le=2.0)
    top_p: float = Field(default=0.85, ge=0.0, le=1.0)
    max_tokens: int = Field(default=8192, ge=1, le=32768)
    open_thinking: bool = False
    model_weight: Optional[str] = None
    lora_weight: Optional[str] = None
    use_moe: bool = False
    hidden_size: int = 768
    num_hidden_layers: int = 8


class ChatResponse(BaseModel):
    content: str
    reasoning_content: Optional[str] = None
    tool_calls: Optional[List[Dict[str, Any]]] = None


class WSChatRequest(BaseModel):
    messages: List[ChatMessage]
    temperature: float = Field(default=0.85, ge=0.0, le=2.0)
    top_p: float = Field(default=0.85, ge=0.0, le=1.0)
    max_tokens: int = Field(default=8192, ge=1, le=32768)
    open_thinking: bool = False
    model_weight: Optional[str] = None
    lora_weight: Optional[str] = None
    use_moe: bool = False
    hidden_size: int = 768
    num_hidden_layers: int = 8
