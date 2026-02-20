from __future__ import annotations

from functools import lru_cache

from ..rag.config import RagSettings
from ..rag.rag_engine import RagEngine


@lru_cache(maxsize=1)
def get_settings() -> RagSettings:
    return RagSettings()


@lru_cache(maxsize=1)
def get_engine() -> RagEngine:
    return RagEngine(get_settings())
