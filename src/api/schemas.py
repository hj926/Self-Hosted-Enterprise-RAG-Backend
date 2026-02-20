from __future__ import annotations

from typing import Optional, List, Dict, Any
from pydantic import BaseModel, Field


class ErrorResponse(BaseModel):
    error_code: str
    message: str
    details: Optional[Dict[str, Any]] = None
    request_id: Optional[str] = None


class DocumentOut(BaseModel):
    doc_id: str
    filename: str
    uploaded_at: str


class DocumentDetailOut(DocumentOut):
    chunk_count: int = Field(..., ge=0)


class IngestResponse(BaseModel):
    document: DocumentDetailOut


class QueryRequest(BaseModel):
    question: str = Field(..., min_length=1)
    doc_id: Optional[str] = None
    top_k: Optional[int] = Field(default=None, ge=1, le=50)


class CitationOut(BaseModel):
    doc_id: str
    filename: str
    page: int
    snippet: str
    chunk_id: str


class QueryResponse(BaseModel):
    answer: str
    citations: List[CitationOut]
    retrieved_count: int
    timings: Dict[str, float]
