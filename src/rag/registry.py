import json
import os
from typing import Any, Dict, List, Optional


class DocumentRegistry:
    def __init__(self, path: str = "storage/registry.json"):
        self.path = path
        os.makedirs(os.path.dirname(self.path), exist_ok=True)

    def _read_all(self) -> Dict[str, Dict[str, Any]]:
        if not os.path.exists(self.path):
            return {}
        with open(self.path, "r", encoding="utf-8") as f:
            return json.load(f)

    def _write_all(self, data: Dict[str, Dict[str, Any]]) -> None:
        tmp = self.path + ".tmp"
        with open(tmp, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        os.replace(tmp, self.path)

    def upsert(self, doc_id: str, record: Dict[str, Any]) -> None:
        data = self._read_all()
        data[doc_id] = record
        self._write_all(data)

    def get(self, doc_id: str) -> Optional[Dict[str, Any]]:
        data = self._read_all()
        return data.get(doc_id)

    def list(self) -> List[Dict[str, Any]]:
        data = self._read_all()
        items = []
        for doc_id, rec in data.items():
            items.append({"doc_id": doc_id, **rec})
        items.sort(key=lambda x: x.get("uploaded_at", ""), reverse=True)
        return items

    def delete(self, doc_id: str) -> bool:
        data = self._read_all()
        if doc_id not in data:
            return False
        del data[doc_id]
        self._write_all(data)
        return True
