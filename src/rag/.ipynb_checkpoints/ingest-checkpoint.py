import os
import sys
from dotenv import load_dotenv
from src.rag.rag_engine import RAGEngine

load_dotenv()

def main():
    pdf_path = sys.argv[1] if len(sys.argv) > 1 else os.getenv("PDF_PATH", "data/sample.pdf")

    engine = RAGEngine(
        chroma_dir=os.getenv("CHROMA_DIR", "storage/chroma"),
        collection=os.getenv("CHROMA_COLLECTION", "pdf_rag"),
        embedding_model=os.getenv("EMBED_MODEL", "nomic-embed-text"),
        llm_model=os.getenv("OLLAMA_LLM", "llama3.2:3b"),
    )

    n = engine.ingest_pdf(pdf_path)
    print(f"Ingested {n} chunks from {pdf_path}")

if __name__ == "__main__":
    main()
