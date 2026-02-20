import os
from dataclasses import dataclass
from typing import List, Dict, Any, Optional

from langchain_community.document_loaders import PyPDFLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import Chroma
from langchain_ollama import OllamaEmbeddings, OllamaLLM


@dataclass
class SourceChunk:
    idx: int
    source: Optional[str]
    page: Optional[int]
    snippet: str


class RAGEngine:
    def __init__(
        self,
        chroma_dir: str = "storage/chroma",
        collection: str = "pdf_rag",
        embedding_model: str = "nomic-embed-text",
        llm_model: str = "llama3.2:3b",
        chunk_size: int = 1000,
        chunk_overlap: int = 150,
    ):
        self.chroma_dir = chroma_dir
        self.collection = collection
        self.embedding_model = embedding_model
        self.llm_model = llm_model
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap

        self.embeddings = OllamaEmbeddings(model=self.embedding_model)
        self.llm = OllamaLLM(model=self.llm_model)

    def ingest_pdf(self, pdf_path: str) -> int:
        loader = PyPDFLoader(pdf_path)
        docs = loader.load()

        splitter = RecursiveCharacterTextSplitter(
            chunk_size=self.chunk_size,
            chunk_overlap=self.chunk_overlap,
        )
        chunks = splitter.split_documents(docs)

        vectordb = Chroma.from_documents(
            documents=chunks,
            embedding=self.embeddings,
            persist_directory=self.chroma_dir,
            collection_name=self.collection,
        )
        vectordb.persist()
        return len(chunks)

    def _open_db(self) -> Chroma:
        return Chroma(
            persist_directory=self.chroma_dir,
            embedding_function=self.embeddings,
            collection_name=self.collection,
        )

    def query(self, question: str, top_k: int = 4) -> Dict[str, Any]:
        db = self._open_db()
        docs = db.max_marginal_relevance_search(question, k=top_k, fetch_k=max(10, top_k * 3))


        context_blocks = []
sources: List[SourceChunk] = []

seen = set()  
idx = 1       
for d in docs:
    src = d.metadata.get("source")
    page = d.metadata.get("page")
    text = d.page_content.strip()

    
    dedupe_key = (src, page, text[:200])

    if dedupe_key in seen:
        continue

    seen.add(dedupe_key)

    snippet = text[:400].replace("\n", " ")

    sources.append(SourceChunk(idx, src, page, snippet))
    context_blocks.append(f"[{idx}] (source={src}, page={page})\n{text}")

    idx += 1


        context = "\n\n".join(context_blocks)

        # Forced citation: Allows the model to refer to the source using [1][2] in the response.
        prompt = f"""You are a helpful assistant. Answer using ONLY the context.
If you use a fact, cite it with bracketed numbers like [1] or [2] that refer to the context blocks.
If the context is insufficient, say "I don't know based on the provided document."

Question:
{question}

Context:
{context}

Answer (with citations):"""

        answer = self.llm.invoke(prompt)

        return {
            "question": question,
            "answer": answer,
            "sources": [s.__dict__ for s in sources],
        }
