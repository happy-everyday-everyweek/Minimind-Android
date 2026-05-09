import os
import re
import uuid
import asyncio
import signal
import json
from typing import Dict, Optional, Any
from datetime import datetime

from core.config import TRAINING_SCRIPTS, TRAINER_DIR
from core.ws_manager import ws_manager


class TrainingTask:
    def __init__(self, task_id: str, task_type: str, script_path: str, args: list, env: Optional[dict] = None):
        self.task_id = task_id
        self.task_type = task_type
        self.script_path = script_path
        self.args = args
        self.env = env or {}
        self.process: Optional[asyncio.subprocess.Process] = None
        self.status = "pending"
        self.progress: Dict[str, Any] = {}
        self.logs: list = []
        self.created_at = datetime.now().isoformat()
        self.started_at: Optional[str] = None
        self.finished_at: Optional[str] = None
        self.message = ""


class TrainingService:
    def __init__(self):
        self.tasks: Dict[str, TrainingTask] = {}

    def _generate_task_id(self) -> str:
        return uuid.uuid4().hex[:12]

    def _build_common_args(self, params: dict) -> list:
        args = []
        args.extend(["--device", params.get("device", "cpu")])
        args.extend(["--dtype", "float32"])
        args.extend(["--num_workers", "0"])
        args.extend(["--accumulation_steps", "1"])
        args.extend(["--log_interval", "1"])
        return args

    async def _run_training(self, task: TrainingTask):
        task.status = "running"
        task.started_at = datetime.now().isoformat()
        await ws_manager.broadcast_training_status(task.task_id, "running", "训练已启动")

        cmd = ["/usr/bin/env", "python3", task.script_path] + task.args + ["--from_resume", "1"]
        env = os.environ.copy()
        for k, v in task.env.items():
            env[k] = v

        try:
            task.process = await asyncio.create_subprocess_exec(
                *cmd,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.STDOUT,
                cwd=TRAINER_DIR,
                env=env,
                preexec_fn=os.setsid,
            )

            loss_pattern = re.compile(r'loss:\s*([\d.]+)')
            epoch_pattern = re.compile(r'Epoch:\[(\d+)/(\d+)\]\((\d+)/(\d+)\)')
            lr_pattern = re.compile(r'lr:\s*([\d.eE+-]+)')
            reward_pattern = re.compile(r'Reward:\s*([\d.]+)')
            kl_pattern = re.compile(r'KL[_a-z]*:\s*([\d.]+)')

            buffer = ""
            while True:
                chunk = await task.process.stdout.read(256)
                if not chunk:
                    break
                text = chunk.decode("utf-8", errors="replace")
                buffer += text

                while "\n" in buffer:
                    line, buffer = buffer.split("\n", 1)
                    line = line.strip()
                    if not line:
                        continue

                    task.logs.append(line)
                    if len(task.logs) > 10000:
                        task.logs = task.logs[-5000:]

                    epoch_match = epoch_pattern.search(line)
                    if epoch_match:
                        task.progress["current_epoch"] = int(epoch_match.group(1))
                        task.progress["total_epochs"] = int(epoch_match.group(2))
                        task.progress["current_step"] = int(epoch_match.group(3))
                        task.progress["total_steps"] = int(epoch_match.group(4))

                    loss_match = loss_pattern.search(line)
                    if loss_match:
                        task.progress["loss"] = float(loss_match.group(1))

                    lr_match = lr_pattern.search(line)
                    if lr_match:
                        task.progress["learning_rate"] = lr_match.group(1)

                    reward_match = reward_pattern.search(line)
                    if reward_match:
                        task.progress["reward"] = float(reward_match.group(1))

                    kl_match = kl_pattern.search(line)
                    if kl_match:
                        task.progress["kl"] = float(kl_match.group(1))

                    if task.progress:
                        await ws_manager.broadcast_training_progress(task.task_id, task.progress)

            await task.process.wait()

            if task.process.returncode == 0:
                task.status = "completed"
                task.message = "训练完成"
            elif task.status == "stopped":
                task.message = "训练已停止"
            elif task.status == "paused":
                task.message = "训练已暂停"
            else:
                task.status = "failed"
                task.message = f"训练失败，退出码: {task.process.returncode}"

        except Exception as e:
            task.status = "failed"
            task.message = f"训练异常: {str(e)}"

        task.finished_at = datetime.now().isoformat()
        await ws_manager.broadcast_training_status(task.task_id, task.status, task.message)

    async def start_pretrain(self, params: dict) -> str:
        task_id = self._generate_task_id()
        script = TRAINING_SCRIPTS["pretrain"]
        args = self._build_common_args(params)
        args.extend(["--data_path", params.get("data_path", "../dataset/pretrain_t2t_mini.jsonl")])
        args.extend(["--hidden_size", str(params.get("hidden_size", 768))])
        args.extend(["--num_hidden_layers", str(params.get("num_hidden_layers", 8))])
        args.extend(["--use_moe", str(params.get("use_moe", 0))])
        args.extend(["--epochs", str(params.get("epochs", 2))])
        args.extend(["--batch_size", str(params.get("batch_size", 32))])
        args.extend(["--learning_rate", str(params.get("learning_rate", 5e-4))])
        args.extend(["--max_seq_len", str(params.get("max_seq_len", 340))])
        args.extend(["--save_interval", str(params.get("save_interval", 1000))])
        args.extend(["--from_weight", params.get("from_weight", "none")])
        args.extend(["--save_weight", "pretrain"])

        task = TrainingTask(task_id, "pretrain", script, args)
        self.tasks[task_id] = task
        asyncio.create_task(self._run_training(task))
        return task_id

    async def start_sft(self, params: dict) -> str:
        task_id = self._generate_task_id()
        script = TRAINING_SCRIPTS["sft"]
        args = self._build_common_args(params)
        args.extend(["--data_path", params.get("data_path", "../dataset/sft_t2t_mini.jsonl")])
        args.extend(["--hidden_size", str(params.get("hidden_size", 768))])
        args.extend(["--num_hidden_layers", str(params.get("num_hidden_layers", 8))])
        args.extend(["--use_moe", str(params.get("use_moe", 0))])
        args.extend(["--from_weight", params.get("from_weight", "pretrain")])
        args.extend(["--epochs", str(params.get("epochs", 2))])
        args.extend(["--batch_size", str(params.get("batch_size", 16))])
        args.extend(["--learning_rate", str(params.get("learning_rate", 1e-5))])
        args.extend(["--max_seq_len", str(params.get("max_seq_len", 768))])
        args.extend(["--save_interval", str(params.get("save_interval", 1000))])
        args.extend(["--save_weight", "full_sft"])

        task = TrainingTask(task_id, "sft", script, args)
        self.tasks[task_id] = task
        asyncio.create_task(self._run_training(task))
        return task_id

    async def start_lora(self, params: dict) -> str:
        task_id = self._generate_task_id()
        script = TRAINING_SCRIPTS["lora"]
        args = self._build_common_args(params)
        args.extend(["--data_path", params.get("data_path", "../dataset/lora_medical.jsonl")])
        args.extend(["--hidden_size", str(params.get("hidden_size", 768))])
        args.extend(["--num_hidden_layers", str(params.get("num_hidden_layers", 8))])
        args.extend(["--use_moe", str(params.get("use_moe", 0))])
        args.extend(["--from_weight", params.get("from_weight", "full_sft")])
        args.extend(["--lora_name", params.get("lora_name", "lora_medical")])
        args.extend(["--epochs", str(params.get("epochs", 10))])
        args.extend(["--batch_size", str(params.get("batch_size", 32))])
        args.extend(["--learning_rate", str(params.get("learning_rate", 1e-4))])
        args.extend(["--max_seq_len", str(params.get("max_seq_len", 340))])
        args.extend(["--save_interval", str(params.get("save_interval", 1000))])

        task = TrainingTask(task_id, "lora", script, args)
        self.tasks[task_id] = task
        asyncio.create_task(self._run_training(task))
        return task_id

    async def start_dpo(self, params: dict) -> str:
        task_id = self._generate_task_id()
        script = TRAINING_SCRIPTS["dpo"]
        args = self._build_common_args(params)
        args.extend(["--data_path", params.get("data_path", "../dataset/dpo.jsonl")])
        args.extend(["--hidden_size", str(params.get("hidden_size", 768))])
        args.extend(["--num_hidden_layers", str(params.get("num_hidden_layers", 8))])
        args.extend(["--use_moe", str(params.get("use_moe", 0))])
        args.extend(["--from_weight", params.get("from_weight", "full_sft")])
        args.extend(["--beta", str(params.get("beta", 0.15))])
        args.extend(["--epochs", str(params.get("epochs", 1))])
        args.extend(["--batch_size", str(params.get("batch_size", 4))])
        args.extend(["--learning_rate", str(params.get("learning_rate", 4e-8))])
        args.extend(["--max_seq_len", str(params.get("max_seq_len", 1024))])
        args.extend(["--save_interval", str(params.get("save_interval", 100))])
        args.extend(["--save_weight", "dpo"])

        task = TrainingTask(task_id, "dpo", script, args)
        self.tasks[task_id] = task
        asyncio.create_task(self._run_training(task))
        return task_id

    async def start_ppo(self, params: dict) -> str:
        task_id = self._generate_task_id()
        script = TRAINING_SCRIPTS["ppo"]
        args = self._build_common_args(params)
        args.extend(["--data_path", params.get("data_path", "../dataset/rlaif.jsonl")])
        args.extend(["--hidden_size", str(params.get("hidden_size", 768))])
        args.extend(["--num_hidden_layers", str(params.get("num_hidden_layers", 8))])
        args.extend(["--use_moe", str(params.get("use_moe", 0))])
        args.extend(["--from_weight", params.get("from_weight", "full_sft")])
        args.extend(["--clip_epsilon", str(params.get("clip_epsilon", 0.2))])
        args.extend(["--kl_coef", str(params.get("kl_coef", 0.02))])
        args.extend(["--epochs", str(params.get("epochs", 1))])
        args.extend(["--batch_size", str(params.get("batch_size", 2))])
        args.extend(["--learning_rate", str(params.get("learning_rate", 3e-7))])
        args.extend(["--max_seq_len", str(params.get("max_seq_len", 768))])
        args.extend(["--max_gen_len", str(params.get("max_gen_len", 1024))])
        args.extend(["--save_interval", str(params.get("save_interval", 10))])
        args.extend(["--save_weight", "ppo_actor"])

        task = TrainingTask(task_id, "ppo", script, args)
        self.tasks[task_id] = task
        asyncio.create_task(self._run_training(task))
        return task_id

    async def start_grpo(self, params: dict) -> str:
        task_id = self._generate_task_id()
        script = TRAINING_SCRIPTS["grpo"]
        args = self._build_common_args(params)
        args.extend(["--data_path", params.get("data_path", "../dataset/rlaif.jsonl")])
        args.extend(["--hidden_size", str(params.get("hidden_size", 768))])
        args.extend(["--num_hidden_layers", str(params.get("num_hidden_layers", 8))])
        args.extend(["--use_moe", str(params.get("use_moe", 0))])
        args.extend(["--from_weight", params.get("from_weight", "full_sft")])
        args.extend(["--loss_type", params.get("loss_type", "cispo")])
        args.extend(["--beta", str(params.get("beta", 0.1))])
        args.extend(["--epsilon", str(params.get("epsilon", 0.2))])
        args.extend(["--num_generations", str(params.get("num_generations", 6))])
        args.extend(["--epochs", str(params.get("epochs", 1))])
        args.extend(["--batch_size", str(params.get("batch_size", 2))])
        args.extend(["--learning_rate", str(params.get("learning_rate", 3e-7))])
        args.extend(["--max_seq_len", str(params.get("max_seq_len", 768))])
        args.extend(["--max_gen_len", str(params.get("max_gen_len", 1024))])
        args.extend(["--save_interval", str(params.get("save_interval", 10))])
        args.extend(["--save_weight", "grpo"])

        task = TrainingTask(task_id, "grpo", script, args)
        self.tasks[task_id] = task
        asyncio.create_task(self._run_training(task))
        return task_id

    async def start_agent(self, params: dict) -> str:
        task_id = self._generate_task_id()
        script = TRAINING_SCRIPTS["agent"]
        args = self._build_common_args(params)
        args.extend(["--data_path", params.get("data_path", "../dataset/agent_rl.jsonl")])
        args.extend(["--hidden_size", str(params.get("hidden_size", 768))])
        args.extend(["--num_hidden_layers", str(params.get("num_hidden_layers", 8))])
        args.extend(["--use_moe", str(params.get("use_moe", 0))])
        args.extend(["--from_weight", params.get("from_weight", "full_sft")])
        args.extend(["--loss_type", params.get("loss_type", "cispo")])
        args.extend(["--beta", str(params.get("beta", 0.1))])
        args.extend(["--num_generations", str(params.get("num_generations", 4))])
        args.extend(["--max_gen_len", str(params.get("max_gen_len", 768))])
        args.extend(["--epochs", str(params.get("epochs", 1))])
        args.extend(["--batch_size", str(params.get("batch_size", 2))])
        args.extend(["--learning_rate", str(params.get("learning_rate", 3e-7))])
        args.extend(["--max_seq_len", str(params.get("max_seq_len", 1024))])
        args.extend(["--save_interval", str(params.get("save_interval", 10))])
        args.extend(["--save_weight", "agent"])

        env = {}
        if params.get("tools_config"):
            env["MINIMIND_TOOLS_CONFIG"] = params["tools_config"]
        if params.get("reward_config"):
            env["MINIMIND_REWARD_CONFIG"] = params["reward_config"]

        task = TrainingTask(task_id, "agent", script, args, env)
        self.tasks[task_id] = task
        asyncio.create_task(self._run_training(task))
        return task_id

    async def start_distillation(self, params: dict) -> str:
        task_id = self._generate_task_id()
        script = TRAINING_SCRIPTS["distillation"]
        args = self._build_common_args(params)
        args.extend(["--data_path", params.get("data_path", "../dataset/sft_t2t_mini.jsonl")])
        args.extend(["--student_hidden_size", str(params.get("student_hidden_size", 768))])
        args.extend(["--student_num_layers", str(params.get("student_num_layers", 8))])
        args.extend(["--student_use_moe", str(params.get("student_use_moe", 0))])
        args.extend(["--teacher_hidden_size", str(params.get("teacher_hidden_size", 768))])
        args.extend(["--teacher_num_layers", str(params.get("teacher_num_layers", 8))])
        args.extend(["--teacher_use_moe", str(params.get("teacher_use_moe", 1))])
        args.extend(["--from_student_weight", params.get("from_student_weight", "full_sft")])
        args.extend(["--from_teacher_weight", params.get("from_teacher_weight", "full_sft")])
        args.extend(["--alpha", str(params.get("alpha", 0.5))])
        args.extend(["--temperature", str(params.get("temperature", 1.5))])
        args.extend(["--epochs", str(params.get("epochs", 6))])
        args.extend(["--batch_size", str(params.get("batch_size", 32))])
        args.extend(["--learning_rate", str(params.get("learning_rate", 5e-6))])
        args.extend(["--max_seq_len", str(params.get("max_seq_len", 340))])
        args.extend(["--save_interval", str(params.get("save_interval", 100))])
        args.extend(["--save_weight", "full_dist"])

        task = TrainingTask(task_id, "distillation", script, args)
        self.tasks[task_id] = task
        asyncio.create_task(self._run_training(task))
        return task_id

    async def pause_task(self, task_id: str) -> bool:
        task = self.tasks.get(task_id)
        if not task or task.status != "running":
            return False
        if task.process and task.process.returncode is None:
            try:
                os.killpg(os.getpgid(task.process.pid), signal.SIGTSTP)
                task.status = "paused"
                task.message = "训练已暂停"
                await ws_manager.broadcast_training_status(task_id, "paused", "训练已暂停")
                return True
            except Exception:
                return False
        return False

    async def resume_task(self, task_id: str) -> bool:
        task = self.tasks.get(task_id)
        if not task or task.status != "paused":
            return False
        if task.process and task.process.returncode is None:
            try:
                os.killpg(os.getpgid(task.process.pid), signal.SIGCONT)
                task.status = "running"
                task.message = "训练已恢复"
                await ws_manager.broadcast_training_status(task_id, "running", "训练已恢复")
                return True
            except Exception:
                return False
        return False

    async def stop_task(self, task_id: str) -> bool:
        task = self.tasks.get(task_id)
        if not task or task.status not in ("running", "paused"):
            return False
        if task.process and task.process.returncode is None:
            try:
                os.killpg(os.getpgid(task.process.pid), signal.SIGTERM)
                task.status = "stopped"
                task.message = "训练已停止"
                await ws_manager.broadcast_training_status(task_id, "stopped", "训练已停止")
                return True
            except Exception:
                return False
        return False

    async def resume_from_checkpoint(self, task_id: str, checkpoint: str = "") -> str:
        old_task = self.tasks.get(task_id)
        if not old_task:
            raise ValueError("训练任务不存在")
        new_task_id = self._generate_task_id()
        args = list(old_task.args)
        args.extend(["--from_resume", "1"])
        if checkpoint:
            args.extend(["--from_weight", checkpoint])
        task = TrainingTask(new_task_id, old_task.task_type, old_task.script_path, args, old_task.env)
        self.tasks[new_task_id] = task
        asyncio.create_task(self._run_training(task))
        return new_task_id

    def get_task_status(self, task_id: str) -> Optional[dict]:
        task = self.tasks.get(task_id)
        if not task:
            return None
        return {
            "task_id": task.task_id,
            "task_type": task.task_type,
            "status": task.status,
            "progress": task.progress,
            "message": task.message,
            "created_at": task.created_at,
            "started_at": task.started_at,
            "finished_at": task.finished_at,
        }

    def get_task_logs(self, task_id: str, tail: int = 200) -> Optional[list]:
        task = self.tasks.get(task_id)
        if not task:
            return None
        return task.logs[-tail:]


training_service = TrainingService()
