from __future__ import annotations

from fastapi import FastAPI, UploadFile, File, Depends, Request
from fastapi.responses import JSONResponse

from .deps import get_engine, get_settings
from .middleware import request_id_middleware
from .schemas import (
    ErrorResponse,
    IngestResponse,
    QueryRequest,
    QueryResponse,
    CitationOut,
    DocumentOut,
    DocumentDetailOut,
)
from ..rag.errors import RagError
from ..rag.rag_engine import RagEngine


app = FastAPI(title="RAG Service", version="0.1.0")
app.middleware("http")(request_id_middleware)


@app.exception_handler(RagError)
async def rag_error_handler(request: Request, exc: RagError):
    req_id = getattr(request.state, "request_id", None)
    payload = ErrorResponse(
        error_code=exc.error_code,
        message=exc.message,
        details=exc.details,
        request_id=req_id,
    )
    return JSONResponse(status_code=exc.http_status, content=payload.model_dump())


@app.exception_handler(Exception)
async def unhandled_error_handler(request: Request, exc: Exception):
    req_id = getattr(request.state, "request_id", None)
    payload = ErrorResponse(
        error_code="INTERNAL_ERROR",
        message="Internal server error",
        details={"reason": str(exc)},
        request_id=req_id,
    )
    return JSONResponse(status_code=500, content=payload.model_dump())


@app.get("/health")
def health(engine: RagEngine = Depends(get_engine)):
    return engine.health_probe()


@app.post("/documents", response_model=IngestResponse)
async def ingest_document(
    file: UploadFile = File(...),
    engine: RagEngine = Depends(get_engine),
):
    data = await file.read()
    rec = engine.ingest_pdf(data, filename=file.filename or "uploaded.pdf")
    out = DocumentDetailOut(
        doc_id=rec.doc_id,
        filename=rec.filename,
        uploaded_at=rec.uploaded_at,
        chunk_count=len(rec.chunk_ids),
    )
    return IngestResponse(document=out)


@app.get("/documents", response_model=list[DocumentOut])
def list_documents(engine: RagEngine = Depends(get_engine)):
    docs = engine.list_documents()
    return [
        DocumentOut(doc_id=d.doc_id, filename=d.filename, uploaded_at=d.uploaded_at)
        for d in docs
    ]


@app.get("/documents/{doc_id}", response_model=DocumentDetailOut)
def get_document(doc_id: str, engine: RagEngine = Depends(get_engine)):
    d = engine.get_document(doc_id)
    return DocumentDetailOut(
        doc_id=d.doc_id,
        filename=d.filename,
        uploaded_at=d.uploaded_at,
        chunk_count=len(d.chunk_ids),
    )


@app.delete("/documents/{doc_id}", response_model=DocumentOut)
def delete_document(doc_id: str, engine: RagEngine = Depends(get_engine)):
    d = engine.delete_document(doc_id)
    return DocumentOut(doc_id=d.doc_id, filename=d.filename, uploaded_at=d.uploaded_at)


@app.post("/query", response_model=QueryResponse)
def query(req: QueryRequest, engine: RagEngine = Depends(get_engine)):
    result = engine.query(question=req.question, doc_id=req.doc_id, top_k=req.top_k)
    citations = [CitationOut(**c.__dict__) for c in result.citations]
    return QueryResponse(
        answer=result.answer,
        citations=citations,
        retrieved_count=result.retrieved_count,
        timings=result.timings,
    )
