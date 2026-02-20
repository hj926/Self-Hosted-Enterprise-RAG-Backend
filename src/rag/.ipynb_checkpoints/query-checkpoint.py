import os
import sys
from dotenv import load_dotenv
from src.rag.rag_engine import RAGEngine

load_dotenv()

def main():
    question = sys.argv[1] if len(sys.argv) > 1 else "Summarize the key points in 5 bullets."
    top_k = int(os.getenv("TOP_K", "4"))

    engine = RAGEngine(
        chroma_dir=os.getenv("CHROMA_DIR", "storage/chroma"),
        collection=os.getenv("CHROMA_COLLECTION", "pdf_rag"),
        embedding_model=os.getenv("EMBED_MODEL", "nomic-embed-text"),
        llm_model=os.getenv("OLLAMA_LLM", "llama3.2:3b"),
    )

    result = engine.query(question, top_k=top_k)

    print("\n=== Answer ===\n", result["answer"])
    print("\n=== Sources ===")
    for s in result["sources"]:
        print(f'[{s["idx"]}] source={s["source"]} page={s["page"]}')
        print("  snippet:", s["snippet"][:200], "...")
        print()

if __name__ == "__main__":
    main()
