import os
import re
import time
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional, Tuple, Set

from langchain_community.document_loaders import PyPDFLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import Chroma
from langchain_ollama import OllamaEmbeddings, OllamaLLM


_CIT_RE = re.compile(r"\[\d+\]")


@dataclass
class SourceChunk:
    idx: int
    source: Optional[str]
    page: Optional[int]
    snippet: str
    doc_id: Optional[str]
    filename: Optional[str]


class RAGEngine:
    def __init__(
        self,
        chroma_dir: str = "storage/chroma",
        collection: str = "pdf_rag",
        embedding_model: str = "nomic-embed-text",
        llm_model: str = "llama3.2:3b",
        chunk_size: int = 1000,
        chunk_overlap: int = 150,
        strict_rag: bool = True,
    ):
        self.chroma_dir = chroma_dir
        self.collection = collection
        self.embedding_model = embedding_model
        self.llm_model = llm_model
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        self.strict_rag = strict_rag

        self.embeddings = OllamaEmbeddings(model=self.embedding_model)
        self.llm = OllamaLLM(model=self.llm_model)

    def _open_db(self) -> Chroma:
        return Chroma(
            persist_directory=self.chroma_dir,
            embedding_function=self.embeddings,
            collection_name=self.collection,
        )

    def ingest_pdf(self, pdf_path: str) -> Dict[str, Any]:
        doc_id = str(uuid.uuid4())
        uploaded_at = datetime.now(timezone.utc).isoformat()
        filename = os.path.basename(pdf_path)

        loader = PyPDFLoader(pdf_path)
        docs = loader.load()

        splitter = RecursiveCharacterTextSplitter(
            chunk_size=self.chunk_size,
            chunk_overlap=self.chunk_overlap,
        )
        chunks = splitter.split_documents(docs)

        for d in chunks:
            d.metadata["doc_id"] = doc_id
            d.metadata["filename"] = filename
            d.metadata["uploaded_at"] = uploaded_at

        vectordb = Chroma.from_documents(
            documents=chunks,
            embedding=self.embeddings,
            persist_directory=self.chroma_dir,
            collection_name=self.collection,
        )
        vectordb.persist()

        return {
            "doc_id": doc_id,
            "filename": filename,
            "uploaded_at": uploaded_at,
            "chunks": len(chunks),
            "pdf_path": pdf_path,
        }

    def _build_prompt(self, question: str, context: str) -> str:
        return f"""You are a helpful assistant. Answer using ONLY the provided context.
Rules:
- Do NOT use outside knowledge.
- Every non-trivial claim MUST be backed by citations like [1], [2], etc.
- If the context is insufficient, reply exactly: "I don't know based on the provided document."
- Do NOT say "generally accepted" or similar phrases that imply outside knowledge.
- Keep the answer concise and factual.

Question:
{question}

Context:
{context}

Answer (with citations):"""

    def query(
        self, question: str, top_k: int = 4, doc_id: Optional[str] = None
    ) -> Dict[str, Any]:
        t0 = time.perf_counter()
        db = self._open_db()

        fetch_k = max(10, top_k * 3)

        if doc_id:
            docs = db.max_marginal_relevance_search(
                question,
                k=top_k,
                fetch_k=fetch_k,
                filter={"doc_id": doc_id},
            )
        else:
            docs = db.max_marginal_relevance_search(
                question,
                k=top_k,
                fetch_k=fetch_k,
            )

        seen: Set[Tuple[Optional[str], Optional[int], str]] = set()
        sources: List[SourceChunk] = []
        context_blocks: List[str] = []

        i = 0
        for d in docs:
            src = d.metadata.get("source")
            page = d.metadata.get("page")
            docid = d.metadata.get("doc_id")
            filename = d.metadata.get("filename")

            text = (d.page_content or "").strip()
            if not text:
                continue

            key = (src, page, text[:200])
            if key in seen:
                continue
            seen.add(key)

            i += 1
            snippet = text[:400].replace("\n", " ")
            sources.append(SourceChunk(i, src, page, snippet, docid, filename))
            context_blocks.append(
                f"[{i}] (source={src}, page={page}, doc_id={docid})\n{text}"
            )

            if i >= top_k:
                break

        if not context_blocks:
            return {
                "question": question,
                "answer": "I don't know based on the provided document.",
                "sources": [],
                "meta": {
                    "retrieved": 0,
                    "latency_ms": int((time.perf_counter() - t0) * 1000),
                },
            }

        context = "\n\n".join(context_blocks)
        prompt = self._build_prompt(question, context)
        answer = self.llm.invoke(prompt).strip()

        if self.strict_rag:
            if (
                answer != "I don't know based on the provided document."
                and not _CIT_RE.search(answer)
            ):
                answer = "I don't know based on the provided document."

        return {
            "question": question,
            "answer": answer,
            "sources": [s.__dict__ for s in sources],
            "meta": {
                "retrieved": len(sources),
                "latency_ms": int((time.perf_counter() - t0) * 1000),
            },
        }

    def delete_by_doc_id(self, doc_id: str) -> int:
        db = self._open_db()
        col = getattr(db, "_collection", None)
        if col is None:
            return 0

        deleted = 0
        try:
            matches = col.get(where={"doc_id": doc_id}, include=[])
            ids = matches.get("ids", []) if isinstance(matches, dict) else []
            if ids:
                col.delete(ids=ids)
                deleted = len(ids)
            else:
                deleted = 0
        except Exception:
            try:
                col.delete(where={"doc_id": doc_id})
                deleted = -1
            except Exception:
                deleted = 0

        try:
            db.persist()
        except Exception:
            pass

        return deleted

    def ollama_probe(self) -> Dict[str, Any]:
        """
        Performs a minimal real-call probe against Ollama for embeddings and LLM.
        Returns a dict with ok flags and latency metrics.
        """
        result: Dict[str, Any] = {
            "embed_ok": False,
            "llm_ok": False,
            "embed_ms": None,
            "llm_ms": None,
        }

        try:
            t0 = time.perf_counter()
            v = self.embeddings.embed_query("healthcheck")
            result["embed_ms"] = int((time.perf_counter() - t0) * 1000)
            result["embed_ok"] = isinstance(v, list) and len(v) > 0
        except Exception:
            result["embed_ok"] = False

        try:
            t0 = time.perf_counter()
            out = self.llm.invoke("Reply with exactly: OK").strip()
            result["llm_ms"] = int((time.perf_counter() - t0) * 1000)
            result["llm_ok"] = out == "OK"
        except Exception:
            result["llm_ok"] = False

        return result
