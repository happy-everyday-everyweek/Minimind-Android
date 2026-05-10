import os
import json
import uuid
from typing import List, Optional, Dict, Any

from core.config import DATASET_DIR, UPLOAD_DIR


class DatasetService:
    def __init__(self):
        self.upload_dir = UPLOAD_DIR
        self.dataset_dir = DATASET_DIR
        os.makedirs(self.upload_dir, exist_ok=True)

    def list_datasets(self) -> List[dict]:
        datasets = []
        seen_names = set()

        for dir_path in [self.dataset_dir, self.upload_dir]:
            if not os.path.isdir(dir_path):
                continue
            for fname in sorted(os.listdir(dir_path)):
                if not fname.endswith(".jsonl"):
                    continue
                if fname in seen_names:
                    continue
                seen_names.add(fname)
                fpath = os.path.join(dir_path, fname)
                file_size = os.path.getsize(fpath)
                num_lines = 0
                try:
                    with open(fpath, "r", encoding="utf-8") as f:
                        num_lines = sum(1 for _ in f)
                except Exception:
                    pass
                datasets.append({
                    "id": fname.replace(".jsonl", ""),
                    "name": fname,
                    "file_path": fpath,
                    "file_size": file_size,
                    "num_lines": num_lines,
                })

        return datasets

    def get_dataset(self, dataset_id: str) -> Optional[dict]:
        datasets = self.list_datasets()
        for ds in datasets:
            if ds["id"] == dataset_id:
                return ds
        return None

    def upload_dataset(self, filename: str, content: bytes) -> dict:
        safe_name = os.path.basename(filename)
        if not safe_name.endswith(".jsonl"):
            safe_name += ".jsonl"
        file_path = os.path.join(self.upload_dir, safe_name)
        with open(file_path, "wb") as f:
            f.write(content)

        num_lines = 0
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                num_lines = sum(1 for _ in f)
        except Exception:
            pass

        return {
            "id": safe_name.replace(".jsonl", ""),
            "name": safe_name,
            "file_path": file_path,
            "file_size": len(content),
            "num_lines": num_lines,
        }

    def preview_dataset(self, dataset_id: str, num_lines: int = 20) -> Optional[Dict[str, Any]]:
        ds = self.get_dataset(dataset_id)
        if not ds:
            return None
        lines = []
        try:
            with open(ds["file_path"], "r", encoding="utf-8") as f:
                for i, line in enumerate(f):
                    if i >= num_lines:
                        break
                    line = line.strip()
                    if not line:
                        continue
                    try:
                        lines.append(json.loads(line))
                    except json.JSONDecodeError:
                        lines.append({"raw": line})
        except Exception:
            pass
        return {
            "id": dataset_id,
            "lines": lines,
            "total_lines": ds["num_lines"],
        }

    def delete_dataset(self, dataset_id: str) -> bool:
        ds = self.get_dataset(dataset_id)
        if not ds:
            return False
        if ds["file_path"].startswith(self.upload_dir):
            try:
                os.remove(ds["file_path"])
                return True
            except OSError:
                return False
        return False


dataset_service = DatasetService()
