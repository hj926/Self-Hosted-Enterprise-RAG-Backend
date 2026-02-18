import os
from langchain_community.vectorstores import Chroma
from langchain_ollama import OllamaEmbeddings, OllamaLLM

PERSIST_DIR = os.getenv("CHROMA_DIR", "storage/chroma")
COLLECTION = os.getenv("CHROMA_COLLECTION", "pdf_rag")
LLM_MODEL = os.getenv("OLLAMA_LLM", "llama3.2:3b")

def ask(question: str, k: int = 4):
    embeddings = OllamaEmbeddings(model="nomic-embed-text")
    vectordb = Chroma(
        persist_directory=PERSIST_DIR,
        embedding_function=embeddings,
        collection_name=COLLECTION,
    )

    docs = vectordb.similarity_search(question, k=k)

    context_blocks = []
    for i, d in enumerate(docs, 1):
        src = d.metadata.get("source")
        page = d.metadata.get("page")
        context_blocks.append(f"[{i}] (source={src}, page={page})\n{d.page_content}")

    context = "\n\n".join(context_blocks)

    prompt = f"""You are a helpful assistant. Use the provided context to answer the question.
If the context is insufficient, say you don't know.

Question:
{question}

Context:
{context}

Answer:"""

    llm = OllamaLLM(model=LLM_MODEL)
    answer = llm.invoke(prompt)

    print("\n=== Answer ===\n", answer)
    print("\n=== Sources ===")
    for i, d in enumerate(docs, 1):
        print(f"[{i}] source={d.metadata.get('source')} page={d.metadata.get('page')}")

if __name__ == "__main__":
    ask("Summarize the key points of this PDF in 5 bullets.")
