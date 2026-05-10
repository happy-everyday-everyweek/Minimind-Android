import os
import shutil
import glob
from typing import List, Optional

from core.config import OUT_DIR, PRESET_MODELS
from schemas.models import ModelInfo


class ModelService:
    def __init__(self):
        self.out_dir = OUT_DIR

    def list_models(self) -> List[ModelInfo]:
        models = []
        for preset in PRESET_MODELS:
            moe_suffix = "_moe" if preset["use_moe"] else ""
            file_path = os.path.join(
                self.out_dir,
                f"{preset['weight_name']}_{preset['hidden_size']}{moe_suffix}.pth",
            )
            info = ModelInfo(
                id=preset["id"],
                name=preset["name"],
                weight_name=preset["weight_name"],
                hidden_size=preset["hidden_size"],
                num_hidden_layers=preset["num_hidden_layers"],
                use_moe=preset["use_moe"],
                description=preset["description"],
            )
            if os.path.exists(file_path):
                info.file_path = file_path
                info.file_size = os.path.getsize(file_path)
            models.append(info)

        pattern = os.path.join(self.out_dir, "*.pth")
        for pth_file in glob.glob(pattern):
            basename = os.path.basename(pth_file)
            already_listed = any(m.file_path == pth_file for m in models)
            if already_listed:
                continue
            name_parts = basename.replace(".pth", "").split("_")
            weight_name = "_".join(name_parts[:-1]) if len(name_parts) > 1 else basename
            hidden_size = 0
            try:
                hidden_size = int(name_parts[-1])
            except ValueError:
                pass
            info = ModelInfo(
                id=f"custom_{basename}",
                name=basename,
                weight_name=weight_name,
                hidden_size=hidden_size,
                num_hidden_layers=8,
                use_moe="moe" in basename,
                file_path=pth_file,
                file_size=os.path.getsize(pth_file),
            )
            models.append(info)

        lora_dir = os.path.join(self.out_dir, "lora")
        if os.path.isdir(lora_dir):
            for lora_file in glob.glob(os.path.join(lora_dir, "*.pth")):
                basename = os.path.basename(lora_file)
                info = ModelInfo(
                    id=f"lora_{basename}",
                    name=f"LoRA: {basename}",
                    weight_name=basename.replace(".pth", ""),
                    hidden_size=0,
                    num_hidden_layers=0,
                    use_moe=False,
                    description="LoRA权重",
                    file_path=lora_file,
                    file_size=os.path.getsize(lora_file),
                )
                models.append(info)

        return models

    def get_model(self, model_id: str) -> Optional[ModelInfo]:
        models = self.list_models()
        for m in models:
            if m.id == model_id:
                return m
        return None

    def delete_model(self, model_id: str) -> bool:
        model = self.get_model(model_id)
        if not model or not model.file_path:
            return False
        try:
            os.remove(model.file_path)
            return True
        except OSError:
            return False

    def download_model(self, model_id: str) -> Optional[str]:
        preset = None
        for p in PRESET_MODELS:
            if p["id"] == model_id:
                preset = p
                break
        if not preset:
            return None

        moe_suffix = "_moe" if preset["use_moe"] else ""
        file_path = os.path.join(
            self.out_dir,
            f"{preset['weight_name']}_{preset['hidden_size']}{moe_suffix}.pth",
        )
        if os.path.exists(file_path):
            return file_path
        return None

    def export_model(self, model_id: str, export_path: str) -> Optional[str]:
        model = self.get_model(model_id)
        if not model or not model.file_path:
            return None
        if not os.path.exists(model.file_path):
            return None
        export_dir = os.path.dirname(export_path)
        if export_dir and not os.path.exists(export_dir):
            os.makedirs(export_dir, exist_ok=True)
        shutil.copy2(model.file_path, export_path)
        return export_path


model_service = ModelService()
