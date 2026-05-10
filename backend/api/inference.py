import json
from fastapi import APIRouter, WebSocket, WebSocketDisconnect, HTTPException

from schemas.inference import ChatRequest, ChatResponse, WSChatRequest
from services.inference_service import inference_service
from core.ws_manager import ws_manager

router = APIRouter(prefix="/api/inference", tags=["inference"])


@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    if not inference_service.is_loaded():
        try:
            inference_service.load_model(
                weight_name=request.model_weight or "full_sft",
                hidden_size=request.hidden_size,
                num_hidden_layers=request.num_hidden_layers,
                use_moe=request.use_moe,
                lora_weight=request.lora_weight,
            )
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"模型加载失败: {str(e)}")

    try:
        messages = [{"role": m.role, "content": m.content} for m in request.messages]
        result = inference_service.chat(
            messages=messages,
            temperature=request.temperature,
            top_p=request.top_p,
            max_tokens=request.max_tokens,
            open_thinking=request.open_thinking,
        )
        return ChatResponse(**result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.websocket("/ws/chat")
async def ws_chat(websocket: WebSocket):
    await ws_manager.connect(websocket, "inference")
    try:
        while True:
            data = await websocket.receive_text()
            try:
                request = WSChatRequest(**json.loads(data))
            except Exception as e:
                await ws_manager.send_inference_error(websocket, f"请求格式错误: {str(e)}")
                continue

            if not inference_service.is_loaded():
                try:
                    inference_service.load_model(
                        weight_name=request.model_weight or "full_sft",
                        hidden_size=request.hidden_size,
                        num_hidden_layers=request.num_hidden_layers,
                        use_moe=request.use_moe,
                        lora_weight=request.lora_weight,
                    )
                except Exception as e:
                    await ws_manager.send_inference_error(websocket, f"模型加载失败: {str(e)}")
                    continue

            try:
                messages = [{"role": m.role, "content": m.content} for m in request.messages]
                full_text = ""
                for token_type, content in inference_service.chat_stream(
                    messages=messages,
                    temperature=request.temperature,
                    top_p=request.top_p,
                    max_tokens=request.max_tokens,
                    open_thinking=request.open_thinking,
                ):
                    if token_type == "thinking":
                        await ws_manager.send_inference_token(websocket, content, is_thinking=True)
                        full_text += content
                    elif token_type == "content":
                        await ws_manager.send_inference_token(websocket, content, is_thinking=False)
                        full_text += content
                    elif token_type == "tool_calls":
                        await ws_manager.send_to(websocket, {
                            "type": "tool_calls",
                            "tool_calls": content,
                        })

                await ws_manager.send_inference_end(websocket, full_text)
            except Exception as e:
                await ws_manager.send_inference_error(websocket, str(e))

    except WebSocketDisconnect:
        ws_manager.disconnect(websocket, "inference")
