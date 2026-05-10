import os
import json
import re
import asyncio
import uuid
from typing import Optional, Dict, Any
from datetime import datetime

from core.config import LLM_API_CONFIG_PATH, OUT_DIR, TRAINER_DIR, TRAINING_SCRIPTS, DATASET_DIR
from core.ws_manager import ws_manager


class DistillationService:
    def __init__(self):
        self.tasks: Dict[str, dict] = {}

    def load_llm_api_config(self) -> dict:
        if os.path.exists(LLM_API_CONFIG_PATH):
            try:
                with open(LLM_API_CONFIG_PATH, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception:
                pass
        return {
            "api_base": "",
            "api_key": "",
            "model_name": "",
        }

    def save_llm_api_config(self, config: dict):
        with open(LLM_API_CONFIG_PATH, "w", encoding="utf-8") as f:
            json.dump(config, f, ensure_ascii=False, indent=2)

    async def test_llm_api(self, config: dict) -> dict:
        try:
            from openai import OpenAI
            client = OpenAI(
                api_key=config.get("api_key", ""),
                base_url=config.get("api_base", ""),
            )
            response = client.chat.completions.create(
                model=config.get("model_name", ""),
                messages=[{"role": "user", "content": "Hello"}],
                max_tokens=10,
            )
            return {
                "success": True,
                "message": "连接成功",
                "response": response.choices[0].message.content,
            }
        except Exception as e:
            return {
                "success": False,
                "message": f"连接失败: {str(e)}",
            }

    async def _generate_teacher_responses(self, data_path: str, llm_config: dict) -> str:
        if data_path.startswith("../dataset/"):
            abs_data_path = os.path.join(DATASET_DIR, os.path.basename(data_path))
        else:
            abs_data_path = data_path if os.path.isabs(data_path) else os.path.join(DATASET_DIR, data_path)

        if not os.path.exists(abs_data_path):
            raise ValueError(f"数据集文件不存在: {abs_data_path}")

        from openai import OpenAI
        client = OpenAI(
            api_key=llm_config.get("api_key", ""),
            base_url=llm_config.get("api_base", ""),
        )
        model_name = llm_config.get("model_name", "")

        output_dir = os.path.join(OUT_DIR, "distillation_teacher")
        os.makedirs(output_dir, exist_ok=True)
        basename = os.path.basename(abs_data_path).replace(".jsonl", "_teacher.jsonl")
        output_path = os.path.join(output_dir, basename)

        teacher_data = []
        with open(abs_data_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line:
                    teacher_data.append(json.loads(line))

        results = []
        for idx, item in enumerate(teacher_data):
            prompt = item.get("prompt", item.get("input", item.get("instruction", "")))
            if not prompt:
                continue
            try:
                response = client.chat.completions.create(
                    model=model_name,
                    messages=[{"role": "user", "content": prompt}],
                    max_tokens=512,
                )
                teacher_response = response.choices[0].message.content
            except Exception as e:
                teacher_response = ""

            result_item = dict(item)
            result_item["teacher_response"] = teacher_response
            results.append(result_item)

        with open(output_path, "w", encoding="utf-8") as f:
            for item in results:
                f.write(json.dumps(item, ensure_ascii=False) + "\n")

        return output_path

    async def start_blackbox_distillation(
        self,
        data_path: str,
        from_weight: str = "pretrain",
        hidden_size: int = 768,
        num_hidden_layers: int = 8,
        use_moe: bool = False,
        epochs: int = 2,
        batch_size: int = 16,
        learning_rate: float = 1e-5,
        max_seq_len: int = 768,
        save_interval: int = 1000,
        device: str = "cpu",
    ) -> str:
        task_id = uuid.uuid4().hex[:12]
        llm_config = self.load_llm_api_config()

        if not llm_config.get("api_base") or not llm_config.get("api_key"):
            raise ValueError("请先配置LLM API")

        teacher_data_path = await self._generate_teacher_responses(data_path, llm_config)

        script = TRAINING_SCRIPTS["sft"]
        args = [
            "--data_path", teacher_data_path,
            "--hidden_size", str(hidden_size),
            "--num_hidden_layers", str(num_hidden_layers),
            "--use_moe", str(1 if use_moe else 0),
            "--from_weight", from_weight,
            "--epochs", str(epochs),
            "--batch_size", str(batch_size),
            "--learning_rate", str(learning_rate),
            "--max_seq_len", str(max_seq_len),
            "--save_interval", str(save_interval),
            "--device", device,
            "--dtype", "float32",
            "--num_workers", "0",
            "--accumulation_steps", "1",
            "--log_interval", "1",
            "--save_weight", "distilled",
        ]

        self.tasks[task_id] = {
            "task_id": task_id,
            "task_type": "distillation",
            "status": "pending",
            "progress": {},
            "logs": [],
            "created_at": datetime.now().isoformat(),
        }

        env = {
            "MINIMIND_LLM_API_BASE": llm_config.get("api_base", ""),
            "MINIMIND_LLM_API_KEY": llm_config.get("api_key", ""),
            "MINIMIND_LLM_MODEL": llm_config.get("model_name", ""),
        }

        asyncio.create_task(self._run_distillation(task_id, script, args, env))
        return task_id

    async def _run_distillation(self, task_id: str, script: str, args: list, env: dict):
        task = self.tasks[task_id]
        task["status"] = "running"
        await ws_manager.broadcast_training_status(task_id, "running", "黑盒知识蒸馏已启动")

        full_env = os.environ.copy()
        full_env.update(env)

        cmd = ["/usr/bin/env", "python3", script] + args
        try:
            process = await asyncio.create_subprocess_exec(
                *cmd,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.STDOUT,
                cwd=TRAINER_DIR,
                env=full_env,
                preexec_fn=os.setsid,
            )

            loss_pattern = re.compile(r'loss:\s*([\d.]+)')
            epoch_pattern = re.compile(r'Epoch:\[(\d+)/(\d+)\]\((\d+)/(\d+)\)')

            buffer = ""
            while True:
                chunk = await process.stdout.read(256)
                if not chunk:
                    break
                text = chunk.decode("utf-8", errors="replace")
                buffer += text

                while "\n" in buffer:
                    line, buffer = buffer.split("\n", 1)
                    line = line.strip()
                    if not line:
                        continue
                    task["logs"].append(line)
                    if len(task["logs"]) > 10000:
                        task["logs"] = task["logs"][-5000:]

                    epoch_match = epoch_pattern.search(line)
                    if epoch_match:
                        task["progress"]["current_epoch"] = int(epoch_match.group(1))
                        task["progress"]["total_epochs"] = int(epoch_match.group(2))
                        task["progress"]["current_step"] = int(epoch_match.group(3))
                        task["progress"]["total_steps"] = int(epoch_match.group(4))

                    loss_match = loss_pattern.search(line)
                    if loss_match:
                        task["progress"]["loss"] = float(loss_match.group(1))

                    if task["progress"]:
                        await ws_manager.broadcast_training_progress(task_id, task["progress"])

            await process.wait()

            if process.returncode == 0:
                task["status"] = "completed"
                task["message"] = "黑盒知识蒸馏完成"
            else:
                task["status"] = "failed"
                task["message"] = f"黑盒知识蒸馏失败，退出码: {process.returncode}"

        except Exception as e:
            task["status"] = "failed"
            task["message"] = f"黑盒知识蒸馏异常: {str(e)}"

        task["finished_at"] = datetime.now().isoformat()
        await ws_manager.broadcast_training_status(task_id, task["status"], task.get("message", ""))

    async def generate_reward_function(
        self,
        tools_config: str,
        reward_rules: str,
        scenario_description: str,
    ) -> str:
        llm_config = self.load_llm_api_config()
        if not llm_config.get("api_base") or not llm_config.get("api_key"):
            raise ValueError("请先配置LLM API")

        from openai import OpenAI
        client = OpenAI(
            api_key=llm_config.get("api_key", ""),
            base_url=llm_config.get("api_base", ""),
        )
        model_name = llm_config.get("model_name", "")

        prompt = (
            "你是一个专业的强化学习奖励函数设计专家。请根据以下信息生成一个奖励函数代码。\n\n"
            f"## 工具定义\n{tools_config}\n\n"
            f"## 奖励规则\n{reward_rules}\n\n"
            f"## 场景描述\n{scenario_description}\n\n"
            "请生成一个完整的 Python 奖励函数，函数签名为 `def reward_fn(action, observation, tools) -> float`，"
            "返回一个浮点数作为奖励值。只输出函数代码，不要输出其他内容。"
        )

        response = client.chat.completions.create(
            model=model_name,
            messages=[{"role": "user", "content": prompt}],
            max_tokens=2048,
        )
        return response.choices[0].message.content


distillation_service = DistillationService()
