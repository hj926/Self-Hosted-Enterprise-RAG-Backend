import json
import logging
import os
import time
import uuid
from typing import Optional, List, Dict, Any

from fastapi import FastAPI, HTTPException, Request, Response
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

from src.rag.rag_engine import RAGEngine
from src.rag.registry import DocumentRegistry


def setup_logging() -> None:
    level = os.getenv("LOG_LEVEL", "INFO").upper()
    logging.basicConfig(
        level=level,
        format="%(message)s",
    )


setup_logging()
logger = logging.getLogger("rag_api")

app = FastAPI(title="Self-Hosted Enterprise RAG Backend", version="0.3.0")

engine = RAGEngine(
    chroma_dir=os.getenv("CHROMA_DIR", "storage/chroma"),
    collection=os.getenv("CHROMA_COLLECTION", "pdf_rag"),
    embedding_model=os.getenv("EMBED_MODEL", "nomic-embed-text"),
    llm_model=os.getenv("OLLAMA_LLM", "llama3.2:3b"),
    strict_rag=os.getenv("STRICT_RAG", "true").lower() == "true",
)

registry = DocumentRegistry(path=os.getenv("REGISTRY_PATH", "storage/registry.json"))


def log_event(event: str, **fields: Any) -> None:
    payload = {"event": event, **fields}
    logger.info(json.dumps(payload, ensure_ascii=False))


class ErrorResponse(BaseModel):
    request_id: str
    detail: str


class IngestRequest(BaseModel):
    pdf_path: str = Field(..., description="Path to a local PDF file on the server")


class IngestResponse(BaseModel):
    doc_id: str
    pdf_path: str
    filename: str
    uploaded_at: str
    chunks: int
    status: str


class QueryRequest(BaseModel):
    question: str
    top_k: int = 4
    doc_id: Optional[str] = None


class QueryResponse(BaseModel):
    question: str
    answer: str
    sources: List[Dict[str, Any]]
    meta: Dict[str, Any]


class DocumentRecord(BaseModel):
    doc_id: str
    pdf_path: str
    filename: str
    uploaded_at: str
    chunks: int
    status: str


@app.middleware("http")
async def request_context_middleware(request: Request, call_next):
    request_id = request.headers.get("X-Request-ID") or str(uuid.uuid4())
    request.state.request_id = request_id
    start = time.perf_counter()

    try:
        response: Response = await call_next(request)
    except Exception as e:
        latency_ms = int((time.perf_counter() - start) * 1000)
        log_event(
            "request_error",
            request_id=request_id,
            method=request.method,
            path=str(request.url.path),
            latency_ms=latency_ms,
            error=str(e),
        )
        return JSONResponse(
            status_code=500,
            content={"request_id": request_id, "detail": "Internal Server Error"},
        )

    latency_ms = int((time.perf_counter() - start) * 1000)
    log_event(
        "request",
        request_id=request_id,
        method=request.method,
        path=str(request.url.path),
        status_code=response.status_code,
        latency_ms=latency_ms,
    )
    response.headers["X-Request-ID"] = request_id
    return response


@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    request_id = getattr(request.state, "request_id", str(uuid.uuid4()))
    log_event(
        "http_exception",
        request_id=request_id,
        path=str(request.url.path),
        status_code=exc.status_code,
        detail=str(exc.detail),
    )
    return JSONResponse(
        status_code=exc.status_code,
        content={"request_id": request_id, "detail": str(exc.detail)},
    )


@app.get("/health")
def health(request: Request):
    chroma_dir = os.getenv("CHROMA_DIR", "storage/chroma")
    chroma_ok = os.path.exists(chroma_dir)

    probe = engine.ollama_probe()
    ok = chroma_ok and probe.get("embed_ok") and probe.get("llm_ok")

    log_event(
        "health",
        request_id=request.state.request_id,
        chroma_ok=chroma_ok,
        embed_ok=probe.get("embed_ok"),
        llm_ok=probe.get("llm_ok"),
        embed_ms=probe.get("embed_ms"),
        llm_ms=probe.get("llm_ms"),
    )

    return {
        "status": "ok" if ok else "degraded",
        "chroma_dir": chroma_dir,
        "chroma_ok": chroma_ok,
        "ollama_embed_ok": probe.get("embed_ok"),
        "ollama_llm_ok": probe.get("llm_ok"),
        "embed_ms": probe.get("embed_ms"),
        "llm_ms": probe.get("llm_ms"),
        "strict_rag": engine.strict_rag,
    }


@app.get("/documents", response_model=List[DocumentRecord])
def list_documents(request: Request):
    items = registry.list()
    log_event("documents_list", request_id=request.state.request_id, count=len(items))
    return [DocumentRecord(**x) for x in items]


@app.get("/documents/{doc_id}", response_model=DocumentRecord)
def get_document(doc_id: str, request: Request):
    rec = registry.get(doc_id)
    if not rec:
        raise HTTPException(status_code=404, detail=f"Document not found: {doc_id}")
    log_event("document_get", request_id=request.state.request_id, doc_id=doc_id)
    return DocumentRecord(doc_id=doc_id, **rec)


@app.post("/documents", response_model=IngestResponse)
def ingest(req: IngestRequest, request: Request):
    if not os.path.exists(req.pdf_path):
        raise HTTPException(
            status_code=400, detail=f"pdf_path not found: {req.pdf_path}"
        )
    if not req.pdf_path.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Only .pdf files are supported")

    t0 = time.perf_counter()
    pending_id = "__pending__"

    registry.upsert(
        doc_id=pending_id,
        record={
            "pdf_path": req.pdf_path,
            "filename": os.path.basename(req.pdf_path),
            "uploaded_at": "",
            "chunks": 0,
            "status": "ingesting",
        },
    )

    try:
        result = engine.ingest_pdf(req.pdf_path)
        doc_id = result["doc_id"]

        record = {
            "pdf_path": result["pdf_path"],
            "filename": result["filename"],
            "uploaded_at": result["uploaded_at"],
            "chunks": result["chunks"],
            "status": "ready",
        }
        registry.upsert(doc_id, record)

        if registry.get(pending_id):
            registry.delete(pending_id)

        latency_ms = int((time.perf_counter() - t0) * 1000)
        log_event(
            "document_ingest",
            request_id=request.state.request_id,
            doc_id=doc_id,
            pdf_path=req.pdf_path,
            chunks=result["chunks"],
            latency_ms=latency_ms,
        )

        return IngestResponse(doc_id=doc_id, **record)

    except Exception as e:
        if registry.get(pending_id):
            registry.upsert(
                pending_id,
                {
                    "pdf_path": req.pdf_path,
                    "filename": os.path.basename(req.pdf_path),
                    "uploaded_at": "",
                    "chunks": 0,
                    "status": "failed",
                },
            )
        raise HTTPException(status_code=500, detail=f"ingest failed: {e}")


@app.delete("/documents/{doc_id}")
def delete_document(doc_id: str, request: Request):
    rec = registry.get(doc_id)
    if not rec:
        raise HTTPException(status_code=404, detail=f"Document not found: {doc_id}")

    t0 = time.perf_counter()
    try:
        deleted_count = engine.delete_by_doc_id(doc_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"vector delete failed: {e}")

    registry.delete(doc_id)

    latency_ms = int((time.perf_counter() - t0) * 1000)
    log_event(
        "document_delete",
        request_id=request.state.request_id,
        doc_id=doc_id,
        deleted_count=deleted_count,
        latency_ms=latency_ms,
    )
    return {"status": "deleted", "doc_id": doc_id, "deleted_count": deleted_count}


@app.post("/query", response_model=QueryResponse)
def query(req: QueryRequest, request: Request):
    if req.doc_id and not registry.get(req.doc_id):
        raise HTTPException(status_code=404, detail=f"Document not found: {req.doc_id}")

    t0 = time.perf_counter()
    result = engine.query(req.question, top_k=req.top_k, doc_id=req.doc_id)

    if req.doc_id and (not result.get("sources")):
        raise HTTPException(
            status_code=404,
            detail=f"No chunks found for doc_id={req.doc_id}. Did you ingest the document?",
        )

    latency_ms = int((time.perf_counter() - t0) * 1000)
    meta = result.get("meta") or {}
    meta["api_latency_ms"] = latency_ms
    meta["request_id"] = request.state.request_id

    log_event(
        "query",
        request_id=request.state.request_id,
        doc_id=req.doc_id,
        top_k=req.top_k,
        retrieved=meta.get("retrieved"),
        rag_latency_ms=meta.get("latency_ms"),
        api_latency_ms=latency_ms,
    )

    return QueryResponse(
        question=result["question"],
        answer=result["answer"],
        sources=result["sources"],
        meta=meta,
    )
