import asyncio
import json
from fastapi import WebSocket
from typing import Dict, Set


class WebSocketManager:
    def __init__(self):
        self.active_connections: Dict[str, Set[WebSocket]] = {
            "training": set(),
            "inference": set(),
        }

    async def connect(self, websocket: WebSocket, channel: str = "training"):
        await websocket.accept()
        if channel not in self.active_connections:
            self.active_connections[channel] = set()
        self.active_connections[channel].add(websocket)

    def disconnect(self, websocket: WebSocket, channel: str = "training"):
        if channel in self.active_connections:
            self.active_connections[channel].discard(websocket)

    async def broadcast(self, channel: str, data: dict):
        if channel not in self.active_connections:
            return
        message = json.dumps(data, ensure_ascii=False)
        disconnected = set()
        for ws in self.active_connections[channel]:
            try:
                await ws.send_text(message)
            except Exception:
                disconnected.add(ws)
        for ws in disconnected:
            self.active_connections[channel].discard(ws)

    async def send_to(self, websocket: WebSocket, data: dict):
        try:
            await websocket.send_text(json.dumps(data, ensure_ascii=False))
        except Exception:
            pass

    async def broadcast_training_progress(self, task_id: str, progress: dict):
        await self.broadcast("training", {
            "type": "training_progress",
            "task_id": task_id,
            "data": progress,
        })

    async def broadcast_training_status(self, task_id: str, status: str, message: str = ""):
        await self.broadcast("training", {
            "type": "training_status",
            "task_id": task_id,
            "status": status,
            "message": message,
        })

    async def send_inference_token(self, websocket: WebSocket, token: str, is_thinking: bool = False):
        data = {
            "type": "token",
            "content": token,
            "is_thinking": is_thinking,
        }
        await self.send_to(websocket, data)

    async def send_inference_end(self, websocket: WebSocket, full_text: str):
        data = {
            "type": "end",
            "content": full_text,
        }
        await self.send_to(websocket, data)

    async def send_inference_error(self, websocket: WebSocket, error: str):
        data = {
            "type": "error",
            "message": error,
        }
        await self.send_to(websocket, data)


ws_manager = WebSocketManager()
