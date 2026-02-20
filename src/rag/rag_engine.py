from __future__ import annotations

import hashlib
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Optional

from .config import RagSettings
from .errors import GuardrailViolationError
from .llm_client import OllamaClient
from .pdf_loader import load_pdf_pages
from .registry import DocumentRegistry, DocumentRecord
from .telemetry import Timer, Timings
from .vectorstore import ChromaVectorStore, RetrievedChunk, mmr_select


@dataclass(frozen=True)
class Citation:
    doc_id: str
    filename: str
    page: int
    snippet: str
    chunk_id: str


@dataclass(frozen=True)
class QueryResult:
    answer: str
    citations: list[Citation]
    retrieved_count: int
    timings: dict[str, float]


class RagEngine:
    def __init__(self, settings: RagSettings):
        self.settings = settings
        self.registry = DocumentRegistry(settings.rag_registry_path)
        self.ollama = OllamaClient(
            settings.ollama_base_url, settings.http_timeout_seconds
        )
        self.vs = ChromaVectorStore(settings.rag_chroma_dir)

    def health_probe(self) -> dict[str, Any]:
        self.ollama.health_probe()
        _ = self.ollama.embed(self.settings.ollama_embed_model, "health-check")
        _ = self.ollama.generate(
            self.settings.ollama_llm_model,
            "Reply with: OK",
            temperature=0.0,
            max_tokens=16,
        )
        return {"status": "ok"}

    def ingest_pdf(self, pdf_bytes: bytes, filename: str) -> DocumentRecord:
        t_total = Timer()
        pages = load_pdf_pages(pdf_bytes, filename=filename)

        doc_id = uuid.uuid4().hex
        uploaded_at = datetime.now(timezone.utc).isoformat()

        chunks: list[dict[str, Any]] = []
        for p in pages:
            for c in chunk_text(
                p.text, self.settings.rag_chunk_size, self.settings.rag_chunk_overlap
            ):
                chunks.append({"page": p.page, "text": c})

        ids: list[str] = []
        docs: list[str] = []
        metas: list[dict[str, Any]] = []
        embs: list[list[float]] = []

        timer_embed = Timer()
        for idx, ch in enumerate(chunks):
            chunk_id = make_chunk_id(doc_id, idx, ch["text"])
            ids.append(chunk_id)
            docs.append(ch["text"])
            metas.append(
                {
                    "doc_id": doc_id,
                    "filename": filename,
                    "uploaded_at": uploaded_at,
                    "page": int(ch["page"]),
                }
            )
            embs.append(self.ollama.embed(self.settings.ollama_embed_model, ch["text"]))
        embed_ms = timer_embed.ms()

        timer_vs = Timer()
        self.vs.add(ids=ids, embeddings=embs, documents=docs, metadatas=metas)
        vs_ms = timer_vs.ms()

        rec = self.registry.upsert(doc_id=doc_id, filename=filename, chunk_ids=ids)
        _ = t_total.ms()

        return rec

    def delete_document(self, doc_id: str) -> DocumentRecord:
        rec = self.registry.get(doc_id)
        if rec.chunk_ids:
            self.vs.delete(rec.chunk_ids)
        deleted = self.registry.delete(doc_id)
        return deleted

    def list_documents(self) -> list[DocumentRecord]:
        return self.registry.list()

    def get_document(self, doc_id: str) -> DocumentRecord:
        return self.registry.get(doc_id)

    def query(
        self,
        question: str,
        doc_id: Optional[str] = None,
        top_k: Optional[int] = None,
    ) -> QueryResult:
        timings: dict[str, float] = {}

        t_embed = Timer()
        q_emb = self.ollama.embed(self.settings.ollama_embed_model, question)
        timings["embed_ms"] = t_embed.ms()

        where = {"doc_id": doc_id} if doc_id else None
        raw_top_k = int(top_k or self.settings.rag_top_k)

        t_retrieve = Timer()
        candidates = self.vs.query(
            query_embedding=q_emb, top_k=max(raw_top_k * 2, raw_top_k), where=where
        )
        selected = mmr_select(
            candidates, k=raw_top_k, lambda_mult=self.settings.rag_mmr_lambda
        )
        selected = dedupe_sources(selected)
        timings["retrieve_ms"] = t_retrieve.ms()

        citations = build_citations(selected)

        if self.settings.strict_rag:
            if len(selected) == 0:
                raise GuardrailViolationError("Insufficient context for strict RAG")
            if len(citations) == 0:
                raise GuardrailViolationError("Citations missing under strict RAG")

        prompt = build_prompt(question, citations, strict=self.settings.strict_rag)

        t_gen = Timer()
        answer = self.ollama.generate(
            self.settings.ollama_llm_model,
            prompt,
            temperature=self.settings.llm_temperature,
            max_tokens=self.settings.llm_max_tokens,
        )
        timings["generate_ms"] = t_gen.ms()

        if self.settings.strict_rag and len(citations) == 0:
            raise GuardrailViolationError(
                "Answer produced without citations under strict RAG"
            )

        return QueryResult(
            answer=answer.strip(),
            citations=citations,
            retrieved_count=len(selected),
            timings=timings,
        )


def chunk_text(text: str, chunk_size: int, overlap: int) -> list[str]:
    t = (text or "").strip()
    if not t:
        return []
    chunks: list[str] = []
    start = 0
    n = len(t)
    while start < n:
        end = min(n, start + chunk_size)
        chunks.append(t[start:end])
        if end == n:
            break
        start = max(0, end - overlap)
    return chunks


def make_chunk_id(doc_id: str, idx: int, text: str) -> str:
    h = hashlib.sha1(text.encode("utf-8")).hexdigest()[:10]
    return f"{doc_id}:{idx}:{h}"


def dedupe_sources(chunks: list[RetrievedChunk]) -> list[RetrievedChunk]:
    seen = set()
    out: list[RetrievedChunk] = []
    for c in chunks:
        key = (c.metadata.get("doc_id"), c.metadata.get("page"), c.text[:80])
        if key in seen:
            continue
        seen.add(key)
        out.append(c)
    return out


def build_citations(chunks: list[RetrievedChunk]) -> list[Citation]:
    out: list[Citation] = []
    for c in chunks:
        meta = c.metadata or {}
        doc_id = str(meta.get("doc_id", ""))
        filename = str(meta.get("filename", ""))
        page = int(meta.get("page", 0) or 0)
        snippet = (c.text or "").strip().replace("\n", " ")
        snippet = snippet[:240]
        out.append(
            Citation(
                doc_id=doc_id,
                filename=filename,
                page=page,
                snippet=snippet,
                chunk_id=c.chunk_id,
            )
        )
    return out


def build_prompt(question: str, citations: list[Citation], strict: bool) -> str:
    ctx_lines = []
    for i, cit in enumerate(citations, start=1):
        ctx_lines.append(f"[{i}] (doc_id={cit.doc_id}, page={cit.page}) {cit.snippet}")

    context = "\n".join(ctx_lines)
    policy = (
        "You must answer only using the provided context. "
        "If the context is insufficient, reply exactly: I don't know based on the provided document."
        if strict
        else "Answer using the provided context as primary evidence."
    )

    return (
        f"{policy}\n\n"
        f"Context:\n{context}\n\n"
        f"Question:\n{question}\n\n"
        f"Answer:"
    )
