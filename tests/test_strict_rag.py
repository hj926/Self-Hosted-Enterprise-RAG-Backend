from __future__ import annotations

import pytest

pytest.skip(
    "Requires mocked ollama/vector store for strict RAG unit tests",
    allow_module_level=True,
)
