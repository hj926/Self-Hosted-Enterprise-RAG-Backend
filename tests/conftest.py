from __future__ import annotations

import os
import tempfile
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from src.api.main import app
from src.rag.config import RagSettings
from src.rag.rag_engine import RagEngine


@pytest.fixture()
def temp_storage(monkeypatch):
    with tempfile.TemporaryDirectory() as d:
        base = Path(d)
        chroma = base / "chroma"
        chroma.mkdir(parents=True, exist_ok=True)
        registry = base / "registry.json"

        monkeypatch.setenv("RAG_CHROMA_DIR", str(chroma))
        monkeypatch.setenv("RAG_REGISTRY_PATH", str(registry))
        monkeypatch.setenv("STRICT_RAG", "true")
        yield base


@pytest.fixture()
def client(temp_storage):
    return TestClient(app)
