from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Optional

import chromadb
from chromadb.config import Settings as ChromaSettings

from .errors import VectorStoreError


@dataclass(frozen=True)
class RetrievedChunk:
    chunk_id: str
    text: str
    metadata: dict[str, Any]
    score: float


class ChromaVectorStore:
    def __init__(self, persist_dir: str, collection_name: str = "documents"):
        try:
            self.client = chromadb.PersistentClient(
                path=persist_dir,
                settings=ChromaSettings(anonymized_telemetry=False),
            )
            self.collection = self.client.get_or_create_collection(name=collection_name)
        except Exception as e:
            raise VectorStoreError(str(e))

    def add(
        self,
        ids: list[str],
        embeddings: list[list[float]],
        documents: list[str],
        metadatas: list[dict[str, Any]],
    ) -> None:
        try:
            self.collection.add(
                ids=ids, embeddings=embeddings, documents=documents, metadatas=metadatas
            )
        except Exception as e:
            raise VectorStoreError(str(e))

    def delete(self, ids: list[str]) -> None:
        try:
            self.collection.delete(ids=ids)
        except Exception as e:
            raise VectorStoreError(str(e))

    def query(
        self,
        query_embedding: list[float],
        top_k: int,
        where: Optional[dict[str, Any]] = None,
    ) -> list[RetrievedChunk]:
        try:
            res = self.collection.query(
                query_embeddings=[query_embedding],
                n_results=top_k,
                where=where,
                include=["documents", "metadatas", "distances"],
            )
            ids = (res.get("ids") or [[]])[0]
            docs = (res.get("documents") or [[]])[0]
            metas = (res.get("metadatas") or [[]])[0]
            dists = (res.get("distances") or [[]])[0]

            out: list[RetrievedChunk] = []
            for i in range(len(ids)):
                score = 1.0 - float(dists[i]) if dists[i] is not None else 0.0
                out.append(
                    RetrievedChunk(
                        chunk_id=str(ids[i]),
                        text=str(docs[i]),
                        metadata=dict(metas[i] or {}),
                        score=score,
                    )
                )
            return out
        except Exception as e:
            raise VectorStoreError(str(e))


def mmr_select(
    candidates: list[RetrievedChunk],
    k: int,
    lambda_mult: float,
) -> list[RetrievedChunk]:
    if not candidates:
        return []
    selected: list[RetrievedChunk] = []
    selected.append(candidates[0])
    while len(selected) < min(k, len(candidates)):
        best = None
        best_score = float("-inf")
        for c in candidates:
            if c in selected:
                continue
            relevance = c.score
            diversity = max(similarity(c, s) for s in selected)
            mmr = lambda_mult * relevance - (1 - lambda_mult) * diversity
            if mmr > best_score:
                best_score = mmr
                best = c
        if best is None:
            break
        selected.append(best)
    return selected


def similarity(a: RetrievedChunk, b: RetrievedChunk) -> float:
    if a.chunk_id == b.chunk_id:
        return 1.0
    at = a.text
    bt = b.text
    if not at or not bt:
        return 0.0
    aset = set(at.split())
    bset = set(bt.split())
    inter = len(aset & bset)
    denom = max(1, len(aset | bset))
    return inter / denom
