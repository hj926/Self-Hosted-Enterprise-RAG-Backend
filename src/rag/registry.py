from __future__ import annotations

import json
import os
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Optional

from .errors import DocumentNotFoundError


@dataclass(frozen=True)
class DocumentRecord:
    doc_id: str
    filename: str
    uploaded_at: str
    chunk_ids: list[str]


class DocumentRegistry:
    def __init__(self, registry_path: str):
        self.registry_path = registry_path
        os.makedirs(os.path.dirname(registry_path) or ".", exist_ok=True)
        if not os.path.exists(self.registry_path):
            self._write({"documents": {}})

    def list(self) -> list[DocumentRecord]:
        data = self._read()
        docs = data.get("documents", {})
        out: list[DocumentRecord] = []
        for doc_id, v in docs.items():
            out.append(
                DocumentRecord(
                    doc_id=doc_id,
                    filename=v["filename"],
                    uploaded_at=v["uploaded_at"],
                    chunk_ids=list(v.get("chunk_ids", [])),
                )
            )
        out.sort(key=lambda r: r.uploaded_at, reverse=True)
        return out

    def get(self, doc_id: str) -> DocumentRecord:
        data = self._read()
        docs = data.get("documents", {})
        if doc_id not in docs:
            raise DocumentNotFoundError(doc_id)
        v = docs[doc_id]
        return DocumentRecord(
            doc_id=doc_id,
            filename=v["filename"],
            uploaded_at=v["uploaded_at"],
            chunk_ids=list(v.get("chunk_ids", [])),
        )

    def upsert(
        self, doc_id: str, filename: str, chunk_ids: list[str]
    ) -> DocumentRecord:
        data = self._read()
        docs = data.setdefault("documents", {})
        uploaded_at = datetime.now(timezone.utc).isoformat()
        docs[doc_id] = {
            "filename": filename,
            "uploaded_at": uploaded_at,
            "chunk_ids": chunk_ids,
        }
        self._write(data)
        return DocumentRecord(
            doc_id=doc_id,
            filename=filename,
            uploaded_at=uploaded_at,
            chunk_ids=chunk_ids,
        )

    def delete(self, doc_id: str) -> DocumentRecord:
        data = self._read()
        docs = data.get("documents", {})
        if doc_id not in docs:
            raise DocumentNotFoundError(doc_id)
        v = docs.pop(doc_id)
        self._write(data)
        return DocumentRecord(
            doc_id=doc_id,
            filename=v["filename"],
            uploaded_at=v["uploaded_at"],
            chunk_ids=list(v.get("chunk_ids", [])),
        )

    def _read(self) -> dict[str, Any]:
        with open(self.registry_path, "r", encoding="utf-8") as f:
            return json.load(f)

    def _write(self, data: dict[str, Any]) -> None:
        tmp = self.registry_path + ".tmp"
        with open(tmp, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        os.replace(tmp, self.registry_path)
