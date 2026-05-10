from fastapi import APIRouter, HTTPException

from schemas.models import ModelListResponse, ModelInfo, ModelDownloadRequest, ModelLoadRequest, ModelExportRequest
from services.model_service import model_service
from services.inference_service import inference_service

router = APIRouter(prefix="/api/models", tags=["models"])


@router.get("", response_model=ModelListResponse)
async def list_models():
    models = model_service.list_models()
    for m in models:
        m.is_loaded = (
            inference_service.is_loaded()
            and inference_service.current_weight == m.weight_name
        )
    return ModelListResponse(models=models)


@router.get("/{model_id}", response_model=ModelInfo)
async def get_model(model_id: str):
    model = model_service.get_model(model_id)
    if not model:
        raise HTTPException(status_code=404, detail="模型不存在")
    model.is_loaded = (
        inference_service.is_loaded()
        and inference_service.current_weight == model.weight_name
    )
    return model


@router.delete("/{model_id}")
async def delete_model(model_id: str):
    success = model_service.delete_model(model_id)
    if success:
        return {"message": "模型已删除"}
    raise HTTPException(status_code=400, detail="删除模型失败，模型可能不存在或为内置模型")


@router.post("/download")
async def download_model(request: ModelDownloadRequest):
    file_path = model_service.download_model(request.model_id)
    if file_path:
        return {"message": "模型权重已存在", "file_path": file_path}
    raise HTTPException(status_code=404, detail="模型不存在或权重文件不可用")


@router.post("/load")
async def load_model(request: ModelLoadRequest):
    try:
        inference_service.load_model(
            weight_name=request.weight_name,
            hidden_size=request.hidden_size,
            num_hidden_layers=request.num_hidden_layers,
            use_moe=request.use_moe,
            lora_weight=request.lora_weight,
            max_seq_len=request.max_seq_len,
            inference_rope_scaling=request.inference_rope_scaling,
        )
        return {"message": "模型加载成功", "weight_name": request.weight_name}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"模型加载失败: {str(e)}")


@router.post("/{model_id}/export")
async def export_model(model_id: str, request: ModelExportRequest):
    result = model_service.export_model(model_id, request.export_path)
    if result:
        return {"message": "模型权重导出成功", "export_path": result}
    raise HTTPException(status_code=400, detail="模型导出失败，模型不存在或权重文件不可用")
