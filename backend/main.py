import sys
import os

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from api.inference import router as inference_router
from api.training import router as training_router
from api.models import router as models_router
from api.datasets import router as datasets_router
from api.config import router as config_router
from services.inference_service import inference_service


@asynccontextmanager
async def lifespan(app: FastAPI):
    try:
        inference_service.load_model(
            weight_name="full_sft",
            hidden_size=768,
            num_hidden_layers=8,
            use_moe=False,
        )
        print("[启动] 默认模型 full_sft 加载成功")
    except Exception as e:
        print(f"[启动] 默认模型加载失败（可后续手动加载）: {e}")

    yield

    inference_service.unload_model()
    print("[关闭] 模型已卸载")


app = FastAPI(
    title="MiniMind Backend",
    description="MiniMind 模型训练与推理后端服务",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(inference_router)
app.include_router(training_router)
app.include_router(models_router)
app.include_router(datasets_router)
app.include_router(config_router)


@app.get("/api/health")
async def health_check():
    return {
        "status": "ok",
        "model_loaded": inference_service.is_loaded(),
        "current_weight": inference_service.current_weight,
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="127.0.0.1",
        port=8000,
        reload=False,
        log_level="info",
    )
