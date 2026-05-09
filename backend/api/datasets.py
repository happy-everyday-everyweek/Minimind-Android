from fastapi import APIRouter, HTTPException, UploadFile, File

from schemas.datasets import DatasetListResponse, DatasetPreviewResponse
from services.dataset_service import dataset_service

router = APIRouter(prefix="/api/datasets", tags=["datasets"])


@router.get("", response_model=DatasetListResponse)
async def list_datasets():
    datasets = dataset_service.list_datasets()
    return DatasetListResponse(datasets=datasets)


@router.post("/upload")
async def upload_dataset(file: UploadFile = File(...)):
    if not file.filename.endswith(".jsonl"):
        raise HTTPException(status_code=400, detail="仅支持 .jsonl 格式的数据集文件")
    content = await file.read()
    result = dataset_service.upload_dataset(file.filename, content)
    return {"message": "数据集上传成功", "dataset": result}


@router.get("/{dataset_id}/preview", response_model=DatasetPreviewResponse)
async def preview_dataset(dataset_id: str, num_lines: int = 20):
    result = dataset_service.preview_dataset(dataset_id, num_lines)
    if not result:
        raise HTTPException(status_code=404, detail="数据集不存在")
    return DatasetPreviewResponse(**result)


@router.delete("/{dataset_id}")
async def delete_dataset(dataset_id: str):
    success = dataset_service.delete_dataset(dataset_id)
    if success:
        return {"message": "数据集已删除"}
    raise HTTPException(status_code=400, detail="删除数据集失败，数据集可能不存在或为内置数据集")
