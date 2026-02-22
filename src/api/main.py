from __future__ import annotations

import time
import uuid
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, TimeoutError as FuturesTimeoutError

from fastapi import FastAPI, UploadFile, File, Depends, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel

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
from ..rag.config import RagSettings
from ..rag.llm_client import OllamaClient
from ..rag.errors import RagError
from ..rag.rag_engine import RagEngine

app = FastAPI(title="RAG Service", version="0.1.0")
app.middleware("http")(request_id_middleware)


class HealthResponse(BaseModel):
    status: str
    mode: str
    storage_dir: str
    storage_ok: bool
    ollama_base_url: str
    ollama_embed_model: str
    ollama_llm_model: str
    ollama_ok: bool
    ollama_embed_ok: bool | None
    ollama_llm_ok: bool | None
    details: dict


_DEEP_CACHE = {
    "ts": 0.0,
    "resp": None,
}

# Demo optimized configuration: 5-minute cache, 30-second timeout limit
_DEEP_COOLDOWN_SECONDS = 300
_DEEP_TIMEOUT_SECONDS = 30

_EXECUTOR = ThreadPoolExecutor(max_workers=2)


def _deep_probe(ollama: OllamaClient, settings: RagSettings) -> tuple[bool, bool, dict]:
    detail = {}
    embed_ok = False
    llm_ok = False

    try:
        _ = ollama.embed(settings.ollama_embed_model, "health-check")
        embed_ok = True
    except Exception as e:
        detail["embed_error"] = str(e)

    try:
        # 极简生成任务，max_tokens 降至 4
        _ = ollama.generate(
            settings.ollama_llm_model,
            "OK",
            temperature=0.0,
            max_tokens=4,
        )
        llm_ok = True
    except Exception as e:
        detail["llm_error"] = str(e)

    return embed_ok, llm_ok, detail


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


def _health(deep: bool) -> HealthResponse:
    settings = RagSettings()

    storage_dir = Path(settings.rag_chroma_dir).parent
    storage_ok = False
    storage_detail = {}

    try:
        storage_dir.mkdir(parents=True, exist_ok=True)
        probe = storage_dir / f".health_probe_{uuid.uuid4().hex}"
        probe.write_text("ok", encoding="utf-8")
        probe.unlink(missing_ok=True)
        storage_ok = True
    except Exception as e:
        storage_detail = {"error": str(e)}

    ollama = OllamaClient(settings.ollama_base_url, settings.http_timeout_seconds)

    ollama_ok = False
    ollama_embed_ok = None
    ollama_llm_ok = None
    ollama_detail = {}

    try:
        ollama.health_probe()
        ollama_ok = True
    except Exception as e:
        ollama_detail["health_probe_error"] = str(e)

    if deep:
        now = time.time()
        cached = _DEEP_CACHE.get("resp")
        cached_ts = float(_DEEP_CACHE.get("ts") or 0.0)

        # 检查 5 分钟缓存
        if cached is not None and (now - cached_ts) < _DEEP_COOLDOWN_SECONDS:
            return cached

        if ollama_ok:
            try:
                fut = _EXECUTOR.submit(_deep_probe, ollama, settings)
                # 宽容的 30 秒超时
                embed_ok, llm_ok, probe_detail = fut.result(
                    timeout=_DEEP_TIMEOUT_SECONDS
                )
                ollama_embed_ok = embed_ok
                ollama_llm_ok = llm_ok
                if probe_detail:
                    ollama_detail.update(probe_detail)
            except FuturesTimeoutError:
                ollama_embed_ok = False
                ollama_llm_ok = False
                ollama_detail["deep_timeout_seconds"] = _DEEP_TIMEOUT_SECONDS
            except Exception as e:
                ollama_embed_ok = False
                ollama_llm_ok = False
                ollama_detail["deep_error"] = str(e)

    if deep:
        ok = (
            storage_ok
            and ollama_ok
            and (ollama_embed_ok is True)
            and (ollama_llm_ok is True)
        )
        mode = "deep"
    else:
        ok = storage_ok and ollama_ok
        mode = "quick"

    status = "ok" if ok else "degraded"

    resp = HealthResponse(
        status=status,
        mode=mode,
        storage_dir=str(storage_dir),
        storage_ok=storage_ok,
        ollama_base_url=settings.ollama_base_url,
        ollama_embed_model=settings.ollama_embed_model,
        ollama_llm_model=settings.ollama_llm_model,
        ollama_ok=ollama_ok,
        ollama_embed_ok=ollama_embed_ok,
        ollama_llm_ok=ollama_llm_ok,
        details={"storage": storage_detail, "ollama": ollama_detail},
    )

    if deep:
        _DEEP_CACHE["ts"] = time.time()
        _DEEP_CACHE["resp"] = resp

    return resp


@app.get("/health", response_model=HealthResponse)
def health():
    return _health(deep=False)


@app.get("/health/deep", response_model=HealthResponse)
def health_deep():
    return _health(deep=True)


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
