from fastapi import APIRouter, WebSocket, WebSocketDisconnect, HTTPException

from schemas.training import (
    PretrainRequest, SFTRequest, LoRARequest, DPORequest,
    PPORequest, GRPORequest, AgentRLRequest, DistillationRequest,
    TrainingStatusResponse, TrainingLogsResponse,
)
from services.training_service import training_service
from services.distillation_service import distillation_service
from core.ws_manager import ws_manager

router = APIRouter(prefix="/api/training", tags=["training"])


@router.post("/pretrain")
async def start_pretrain(request: PretrainRequest):
    try:
        task_id = await training_service.start_pretrain(request.model_dump())
        return {"task_id": task_id, "message": "预训练已启动"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/sft")
async def start_sft(request: SFTRequest):
    try:
        task_id = await training_service.start_sft(request.model_dump())
        return {"task_id": task_id, "message": "SFT训练已启动"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/distillation")
async def start_distillation(request: DistillationRequest):
    try:
        task_id = await training_service.start_distillation(request.model_dump())
        return {"task_id": task_id, "message": "知识蒸馏训练已启动"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/lora")
async def start_lora(request: LoRARequest):
    try:
        task_id = await training_service.start_lora(request.model_dump())
        return {"task_id": task_id, "message": "LoRA微调已启动"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/rl")
async def start_rl(request: dict):
    rl_type = request.get("rl_type", "dpo")
    try:
        if rl_type == "dpo":
            task_id = await training_service.start_dpo(request)
        elif rl_type == "ppo":
            task_id = await training_service.start_ppo(request)
        elif rl_type == "grpo":
            task_id = await training_service.start_grpo(request)
        else:
            raise HTTPException(status_code=400, detail=f"不支持的RL类型: {rl_type}")
        return {"task_id": task_id, "message": f"{rl_type.upper()}训练已启动"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/agent")
async def start_agent(request: AgentRLRequest):
    try:
        task_id = await training_service.start_agent(request.model_dump())
        return {"task_id": task_id, "message": "Agentic RL训练已启动"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/{task_id}/pause")
async def pause_training(task_id: str):
    success = await training_service.pause_task(task_id)
    if success:
        return {"task_id": task_id, "message": "训练已暂停"}
    raise HTTPException(status_code=400, detail="无法暂停训练，任务可能不在运行状态")


@router.post("/{task_id}/resume")
async def resume_training(task_id: str):
    success = await training_service.resume_task(task_id)
    if success:
        return {"task_id": task_id, "message": "训练已恢复"}
    raise HTTPException(status_code=400, detail="无法恢复训练，任务可能不在暂停状态")


@router.post("/{task_id}/stop")
async def stop_training(task_id: str):
    success = await training_service.stop_task(task_id)
    if success:
        return {"task_id": task_id, "message": "训练已停止"}
    raise HTTPException(status_code=400, detail="无法停止训练，任务可能不在运行或暂停状态")


@router.post("/{task_id}/resume-from-checkpoint")
async def resume_from_checkpoint(task_id: str, request: dict = None):
    checkpoint = ""
    if request:
        checkpoint = request.get("checkpoint", "")
    try:
        new_task_id = await training_service.resume_from_checkpoint(task_id, checkpoint)
        return {"task_id": new_task_id, "message": "已从断点恢复训练"}
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/{task_id}/status", response_model=TrainingStatusResponse)
async def get_training_status(task_id: str):
    status = training_service.get_task_status(task_id)
    if not status:
        raise HTTPException(status_code=404, detail="训练任务不存在")
    return TrainingStatusResponse(**status)


@router.get("/{task_id}/logs", response_model=TrainingLogsResponse)
async def get_training_logs(task_id: str, tail: int = 200):
    logs = training_service.get_task_logs(task_id, tail)
    if logs is None:
        raise HTTPException(status_code=404, detail="训练任务不存在")
    return TrainingLogsResponse(task_id=task_id, logs=logs)


@router.websocket("/ws/progress")
async def ws_training_progress(websocket: WebSocket):
    await ws_manager.connect(websocket, "training")
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        ws_manager.disconnect(websocket, "training")
