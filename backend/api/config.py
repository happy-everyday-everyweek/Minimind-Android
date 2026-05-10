from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional
import json
import os

from services.distillation_service import distillation_service
from core.config import RESOURCE_LIMITS_CONFIG_PATH, DEFAULT_RESOURCE_LIMITS

router = APIRouter(prefix="/api/config", tags=["config"])


class LLMApiConfig(BaseModel):
    api_base: str = ""
    api_key: str = ""
    model_name: str = ""


class GenerateRewardFunctionRequest(BaseModel):
    tools_config: str = ""
    reward_rules: str = ""
    scenario_description: str = ""


@router.get("/llm-api")
async def get_llm_api_config():
    config = distillation_service.load_llm_api_config()
    masked = {
        "api_base": config.get("api_base", ""),
        "api_key": _mask_key(config.get("api_key", "")),
        "model_name": config.get("model_name", ""),
    }
    return masked


@router.put("/llm-api")
async def update_llm_api_config(config: LLMApiConfig):
    distillation_service.save_llm_api_config(config.model_dump())
    return {"message": "LLM API配置已更新"}


@router.post("/llm-api/test")
async def test_llm_api_config(config: LLMApiConfig):
    result = await distillation_service.test_llm_api(config.model_dump())
    if result["success"]:
        return result
    raise HTTPException(status_code=400, detail=result["message"])


@router.post("/generate-reward-function")
async def generate_reward_function(request: GenerateRewardFunctionRequest):
    try:
        code = await distillation_service.generate_reward_function(
            tools_config=request.tools_config,
            reward_rules=request.reward_rules,
            scenario_description=request.scenario_description,
        )
        return {"code": code}
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"生成奖励函数失败: {str(e)}")


def _mask_key(key: str) -> str:
    if not key or len(key) <= 8:
        return "*" * len(key) if key else ""
    return key[:4] + "*" * (len(key) - 8) + key[-4:]


class ResourceLimitsConfig(BaseModel):
    max_cpu_percent: int = 80
    max_memory_mb: int = 2048
    max_training_processes: int = 1


def _load_resource_limits() -> dict:
    if os.path.exists(RESOURCE_LIMITS_CONFIG_PATH):
        try:
            with open(RESOURCE_LIMITS_CONFIG_PATH, "r", encoding="utf-8") as f:
                saved = json.load(f)
                return {**DEFAULT_RESOURCE_LIMITS, **saved}
        except Exception:
            pass
    return dict(DEFAULT_RESOURCE_LIMITS)


def _save_resource_limits(config: dict):
    with open(RESOURCE_LIMITS_CONFIG_PATH, "w", encoding="utf-8") as f:
        json.dump(config, f, ensure_ascii=False, indent=2)


@router.get("/resource-limits")
async def get_resource_limits():
    return _load_resource_limits()


@router.put("/resource-limits")
async def update_resource_limits(config: ResourceLimitsConfig):
    data = config.model_dump()
    if data["max_cpu_percent"] < 1 or data["max_cpu_percent"] > 100:
        raise HTTPException(status_code=400, detail="CPU 使用率上限必须在 1-100 之间")
    if data["max_memory_mb"] < 256:
        raise HTTPException(status_code=400, detail="内存使用上限不能低于 256MB")
    if data["max_training_processes"] < 1:
        raise HTTPException(status_code=400, detail="最大同时训练进程数不能小于 1")
    _save_resource_limits(data)
    return {"message": "资源限制配置已更新"}
