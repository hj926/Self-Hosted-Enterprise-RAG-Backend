from __future__ import annotations

import re
import threading
from pathlib import Path
from functools import lru_cache

from fastapi import Request

from ..rag.config import RagSettings
from ..rag.errors import TenantRequiredError, RagError
from ..rag.rag_engine import RagEngine


TENANT_HEADER = "x-tenant-id"
TENANT_RE = re.compile(r"^[a-zA-Z0-9][a-zA-Z0-9_-]{0,63}$")

_engines: dict[str, RagEngine] = {}
_lock = threading.Lock()


@lru_cache(maxsize=1)
def get_settings() -> RagSettings:
    return RagSettings()


def _get_tenant_id(request: Request) -> str:
    tenant_id = request.headers.get(TENANT_HEADER)
    if not tenant_id:
        raise TenantRequiredError()
    tenant_id = tenant_id.strip()
    if not TENANT_RE.match(tenant_id):
        raise RagError(
            error_code="INVALID_TENANT_ID",
            message="Invalid tenant id format",
            http_status=400,
            details={"tenant_id": tenant_id},
        )
    return tenant_id


def get_engine(request: Request) -> RagEngine:
    settings = get_settings()
    tenant_id = _get_tenant_id(request)

    # Derive a stable root from current settings (works for docker + tests)
    chroma_base = Path(settings.rag_chroma_dir)
    root = chroma_base.parent  # e.g., storage/ (or temp dir in tests)

    tenant_root = root / "tenants" / tenant_id
    chroma_dir = tenant_root / "chroma"
    registry_path = tenant_root / "registry.json"

    with _lock:
        eng = _engines.get(tenant_id)
        if eng is None:
            eng = RagEngine(
                settings,
                chroma_dir=str(chroma_dir),
                registry_path=str(registry_path),
            )
            _engines[tenant_id] = eng
        return eng
