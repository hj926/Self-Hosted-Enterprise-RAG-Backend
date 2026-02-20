from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Optional


@dataclass
class RagError(Exception):
    error_code: str
    message: str
    http_status: int = 400
    details: Optional[dict[str, Any]] = None


class DocumentNotFoundError(RagError):
    def __init__(self, doc_id: str):
        super().__init__(
            error_code="DOCUMENT_NOT_FOUND",
            message=f"Document not found: {doc_id}",
            http_status=404,
            details={"doc_id": doc_id},
        )


class PDFParseError(RagError):
    def __init__(self, reason: str, filename: Optional[str] = None):
        super().__init__(
            error_code="PDF_PARSE_FAILED",
            message="Failed to parse PDF",
            http_status=400,
            details={"reason": reason, "filename": filename},
        )


class OllamaUnavailableError(RagError):
    def __init__(self, reason: str):
        super().__init__(
            error_code="OLLAMA_UNAVAILABLE",
            message="Ollama service is unavailable",
            http_status=503,
            details={"reason": reason},
        )


class VectorStoreError(RagError):
    def __init__(self, reason: str):
        super().__init__(
            error_code="VECTORSTORE_ERROR",
            message="Vector store operation failed",
            http_status=500,
            details={"reason": reason},
        )


class UpstreamTimeoutError(RagError):
    def __init__(self, reason: str):
        super().__init__(
            error_code="UPSTREAM_TIMEOUT",
            message="Upstream request timed out",
            http_status=504,
            details={"reason": reason},
        )


class GuardrailViolationError(RagError):
    def __init__(self, reason: str):
        super().__init__(
            error_code="GUARDRAIL_VIOLATION",
            message="Request could not be answered under strict RAG policy",
            http_status=422,
            details={"reason": reason},
        )
