import os
import subprocess
from typing import Optional, List, Dict, Any

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from src.rag.rag_engine import RAGEngine

app = FastAPI(title="Self-Hosted Enterprise RAG Backend", version="0.1.0")


engine = RAGEngine(
    chroma_dir=os.getenv("CHROMA_DIR", "storage/chroma"),
    collection=os.getenv("CHROMA_COLLECTION", "pdf_rag"),
    embedding_model=os.getenv("EMBED_MODEL", "nomic-embed-text"),
    llm_model=os.getenv("OLLAMA_LLM", "llama3.2:3b"),
)

class IngestRequest(BaseModel):
    pdf_path: str = Field(..., description="Path to a local PDF file on the server")

class IngestResponse(BaseModel):
    pdf_path: str
    chunks: int

class QueryRequest(BaseModel):
    question: str
    top_k: int = 4

class QueryResponse(BaseModel):
    question: str
    answer: str
    sources: List[Dict[str, Any]]

@app.get("/health")
def health():
    # 1) Chroma dir check
    chroma_dir = os.getenv("CHROMA_DIR", "storage/chroma")
    chroma_ok = os.path.exists(chroma_dir)

    # 2) Ollama check (light)
    try:
        out = subprocess.check_output(["ollama", "list"], stderr=subprocess.STDOUT, text=True, timeout=3)
        ollama_ok = True
    except Exception:
        ollama_ok = False
        out = ""

    return {
        "status": "ok",
        "chroma_dir": chroma_dir,
        "chroma_ok": chroma_ok,
        "ollama_ok": ollama_ok,
        "ollama_list_head": "\n".join(out.splitlines()[:5]) if out else "",
    }

@app.post("/documents", response_model=IngestResponse)
def ingest(req: IngestRequest):
    if not os.path.exists(req.pdf_path):
        raise HTTPException(status_code=400, detail=f"pdf_path not found: {req.pdf_path}")
    if not req.pdf_path.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Only .pdf files are supported in Phase 1")

    try:
        chunks = engine.ingest_pdf(req.pdf_path)
        return IngestResponse(pdf_path=req.pdf_path, chunks=chunks)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"ingest failed: {e}")

@app.post("/query", response_model=QueryResponse)
def query(req: QueryRequest):
    try:
        result = engine.query(req.question, top_k=req.top_k)
        return QueryResponse(**result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"query failed: {e}")
