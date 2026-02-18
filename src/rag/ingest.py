import os
from langchain_community.document_loaders import PyPDFLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import Chroma
from langchain_ollama import OllamaEmbeddings

PDF_PATH = os.getenv("PDF_PATH", "data/sample.pdf")
PERSIST_DIR = os.getenv("CHROMA_DIR", "storage/chroma")
COLLECTION = os.getenv("CHROMA_COLLECTION", "pdf_rag")

def ingest(pdf_path: str = PDF_PATH):
    loader = PyPDFLoader(pdf_path)
    docs = loader.load()

    splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=150)
    chunks = splitter.split_documents(docs)

    embeddings = OllamaEmbeddings(model="nomic-embed-text")

    vectordb = Chroma.from_documents(
        documents=chunks,
        embedding=embeddings,
        persist_directory=PERSIST_DIR,
        collection_name=COLLECTION,
    )
    vectordb.persist()
    print(f"Ingested {len(chunks)} chunks into Chroma at {PERSIST_DIR}")

if __name__ == "__main__":
    ingest()
