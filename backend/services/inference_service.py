import re
import torch
from typing import Optional, List, Dict, Any

from core.config import (
    DEFAULT_DEVICE, DEFAULT_DTYPE, DEFAULT_HIDDEN_SIZE,
    DEFAULT_NUM_HIDDEN_LAYERS, DEFAULT_MAX_SEQ_LEN, OUT_DIR, MODEL_DIR
)


class InferenceService:
    def __init__(self):
        self.model = None
        self.tokenizer = None
        self.current_weight = None
        self.current_config = {}
        self.device = DEFAULT_DEVICE

    def is_loaded(self) -> bool:
        return self.model is not None and self.tokenizer is not None

    def load_model(
        self,
        weight_name: str = "full_sft",
        hidden_size: int = DEFAULT_HIDDEN_SIZE,
        num_hidden_layers: int = DEFAULT_NUM_HIDDEN_LAYERS,
        use_moe: bool = False,
        lora_weight: Optional[str] = None,
        max_seq_len: int = DEFAULT_MAX_SEQ_LEN,
        inference_rope_scaling: bool = False,
    ):
        from transformers import AutoTokenizer
        from model.model_minimind import MiniMindConfig, MiniMindForCausalLM
        from model.model_lora import apply_lora, load_lora

        lm_config = MiniMindConfig(
            hidden_size=hidden_size,
            num_hidden_layers=num_hidden_layers,
            use_moe=use_moe,
            max_seq_len=max_seq_len,
            inference_rope_scaling=inference_rope_scaling,
        )
        tokenizer = AutoTokenizer.from_pretrained(MODEL_DIR)
        model = MiniMindForCausalLM(lm_config)

        if weight_name != "none":
            moe_suffix = "_moe" if use_moe else ""
            weight_path = f"{OUT_DIR}/{weight_name}_{hidden_size}{moe_suffix}.pth"
            weights = torch.load(weight_path, map_location=self.device, weights_only=True)
            model.load_state_dict(weights, strict=False)

        if lora_weight:
            apply_lora(model)
            lora_path = f"{OUT_DIR}/lora/{lora_weight}_{hidden_size}.pth"
            load_lora(model, lora_path)

        model = model.float().eval().to(self.device)
        self.model = model
        self.tokenizer = tokenizer
        self.current_weight = weight_name
        self.current_config = {
            "hidden_size": hidden_size,
            "num_hidden_layers": num_hidden_layers,
            "use_moe": use_moe,
            "max_seq_len": max_seq_len,
        }

    def chat(
        self,
        messages: List[Dict[str, str]],
        temperature: float = 0.85,
        top_p: float = 0.85,
        max_tokens: int = 8192,
        open_thinking: bool = False,
        tools: Optional[list] = None,
    ) -> Dict[str, Any]:
        if not self.is_loaded():
            raise RuntimeError("模型未加载，请先加载模型")

        prompt = self.tokenizer.apply_chat_template(
            messages,
            tokenize=False,
            add_generation_prompt=True,
            tools=tools or None,
            open_thinking=open_thinking,
        )[-max_tokens:]
        inputs = self.tokenizer(prompt, return_tensors="pt", truncation=True).to(self.device)

        with torch.no_grad():
            generated_ids = self.model.generate(
                inputs["input_ids"],
                max_length=inputs["input_ids"].shape[1] + max_tokens,
                do_sample=True,
                attention_mask=inputs["attention_mask"],
                pad_token_id=self.tokenizer.pad_token_id,
                eos_token_id=self.tokenizer.eos_token_id,
                top_p=top_p,
                temperature=temperature,
            )
            answer = self.tokenizer.decode(
                generated_ids[0][inputs["input_ids"].shape[1]:],
                skip_special_tokens=True,
            )

        content, reasoning_content, tool_calls = self._parse_response(answer)
        result = {"content": content}
        if reasoning_content:
            result["reasoning_content"] = reasoning_content
        if tool_calls:
            result["tool_calls"] = tool_calls
        return result

    def chat_stream(
        self,
        messages: List[Dict[str, str]],
        temperature: float = 0.85,
        top_p: float = 0.85,
        max_tokens: int = 8192,
        open_thinking: bool = False,
        tools: Optional[list] = None,
    ):
        from threading import Thread
        from queue import Queue
        from transformers import TextStreamer

        if not self.is_loaded():
            raise RuntimeError("模型未加载，请先加载模型")

        prompt = self.tokenizer.apply_chat_template(
            messages,
            tokenize=False,
            add_generation_prompt=True,
            tools=tools or None,
            open_thinking=open_thinking,
        )[-max_tokens:]
        inputs = self.tokenizer(prompt, return_tensors="pt", truncation=True).to(self.device)

        queue = Queue()

        class QueueStreamer(TextStreamer):
            def __init__(self, tokenizer, q):
                super().__init__(tokenizer, skip_prompt=True, skip_special_tokens=True)
                self.queue = q

            def on_finalized_text(self, text: str, stream_end: bool = False):
                self.queue.put(("text", text))
                if stream_end:
                    self.queue.put(("end", None))

        streamer = QueueStreamer(self.tokenizer, queue)

        def _generate():
            self.model.generate(
                inputs.input_ids,
                max_new_tokens=max_tokens,
                do_sample=True,
                temperature=temperature,
                top_p=top_p,
                attention_mask=inputs.attention_mask,
                pad_token_id=self.tokenizer.pad_token_id,
                eos_token_id=self.tokenizer.eos_token_id,
                streamer=streamer,
            )

        Thread(target=_generate, daemon=True).start()

        full_text = ""
        emitted = 0
        thinking_ended = not open_thinking

        while True:
            msg_type, text = queue.get()
            if msg_type == "end":
                break
            if msg_type == "text":
                full_text += text
                if not thinking_ended:
                    pos = full_text.find("```")
                    if pos >= 0:
                        thinking_ended = True
                        new_r = full_text[emitted:pos]
                        if new_r:
                            yield ("thinking", new_r)
                        emitted = pos + len("```")
                        after = full_text[emitted:].lstrip("\n")
                        emitted = len(full_text) - len(after)
                        if after:
                            yield ("content", after)
                            emitted = len(full_text)
                    else:
                        new_r = full_text[emitted:]
                        if new_r:
                            yield ("thinking", new_r)
                        emitted = len(full_text)
                else:
                    new_c = full_text[emitted:]
                    if new_c:
                        yield ("content", new_c)
                    emitted = len(full_text)

        _, _, tool_calls = self._parse_response(full_text)
        if tool_calls:
            yield ("tool_calls", tool_calls)

    def _parse_response(self, text: str):
        import json
        import time

        reasoning_content = None
        think_match = re.search(r'```(.*?)```', text, re.DOTALL)
        if think_match:
            reasoning_content = think_match.group(1).strip()
            text = re.sub(r'```.*?```\s*', '', text, flags=re.DOTALL)
        elif '```' in text:
            parts = text.split('```', 1)
            reasoning_content = parts[0].strip()
            text = parts[1].strip() if len(parts) > 1 else ''

        tool_calls = []
        for i, m in enumerate(re.findall(r'```(.*?)```', text, re.DOTALL)):
            try:
                call = json.loads(m.strip())
                tool_calls.append({
                    "id": f"call_{int(time.time())}_{i}",
                    "type": "function",
                    "function": {
                        "name": call.get("name", ""),
                        "arguments": json.dumps(call.get("arguments", {}), ensure_ascii=False),
                    },
                })
            except Exception:
                pass
        if tool_calls:
            text = re.sub(r'```.*?```', '', text, flags=re.DOTALL)

        return text.strip(), reasoning_content, tool_calls or None

    def unload_model(self):
        if self.model is not None:
            del self.model
            self.model = None
        if self.tokenizer is not None:
            del self.tokenizer
            self.tokenizer = None
        self.current_weight = None
        self.current_config = {}
        if self.device == "cpu":
            torch.cuda.empty_cache()


inference_service = InferenceService()
