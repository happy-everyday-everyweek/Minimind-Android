from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional

from services.distillation_service import distillation_service

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
