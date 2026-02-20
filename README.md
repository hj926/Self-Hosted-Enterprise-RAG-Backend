# Self‑Hosted Enterprise RAG Backend System

## Overview

This project implements a production‑style, self‑hosted Enterprise Retrieval‑Augmented Generation (RAG) backend system. It combines a Spring Boot backend, a FastAPI‑based Python AI microservice, a vector database, and Ollama‑hosted LLMs. The system is designed using enterprise microservice architecture principles, including asynchronous processing, service decomposition, and secure multi‑tenant access.

The system enables users to upload documents, index them into a vector database, and perform grounded AI queries with citation support.

---

## Architecture

Client
  → Spring Boot Backend API
  → Async Worker
  → Python RAG Service (FastAPI)
  → Ollama (Embedding + LLM)
  → ChromaDB (Vector Database)

Components:

- Spring Boot Backend (Java)
- FastAPI RAG Service (Python)
- Ollama (LLM and Embedding Models)
- ChromaDB (Vector Storage)
- H2 Database (Task persistence)
- Docker Compose Deployment

---

## Key Features

### Enterprise Backend (Spring Boot)

- REST API layer
- Multi‑tenant support via API keys
- Asynchronous ingest task processing
- Background worker polling
- Task state tracking (PENDING, RUNNING, SUCCEEDED, FAILED)
- H2 database persistence
- Secure authentication filter

### AI Service (FastAPI)

- PDF ingestion pipeline
- Text chunking
- Embedding generation (nomic‑embed‑text)
- Vector storage (ChromaDB)
- Query pipeline with LLM (llama3.2:3b)
- Citation‑based grounded answers
- Hallucination prevention guardrails

### Deployment

- Docker‑based microservice orchestration
- Independent backend, rag‑service, and Ollama containers
- Fully self‑hosted environment

---

## Project Structure

Self‑Hosted‑Enterprise‑RAG‑Backend/
│
├── backend/                # Spring Boot backend
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── pom.xml
│
├── src/
│   ├── api/                # FastAPI service
│   │   └── main.py
│   │
│   └── rag/                # RAG pipeline logic
│       ├── ingest.py
│       ├── query.py
│       ├── llm_client.py
│       └── registry.py
│
├── storage/
│   ├── chroma/             # Vector database (ignored in git)
│   └── registry.json
│
├── infra/
│   ├── docker-compose.yml
│   ├── Dockerfile.backend
│   └── Dockerfile.rag
│
├── data/
│   └── sample.pdf
│
├── README.md
├── .gitignore
└── Makefile

---

## API Endpoints

Backend API:

POST /api/v1/documents  
Upload document and create ingest task

GET /api/v1/tasks/{taskId}  
Check ingest task status

POST /api/v1/query  
Query documents

FastAPI RAG Service:

POST /documents  
Ingest document

POST /query  
Run query

GET /health  
Service health check

---

## Task Lifecycle

PENDING → RUNNING → SUCCEEDED / FAILED

---

## Setup Instructions

### 1. Start Docker

docker compose -f infra/docker-compose.yml up --build

---

### 2. Load Ollama Models

docker compose exec ollama ollama pull llama3.2:3b

docker compose exec ollama ollama pull nomic-embed-text

---

### 3. Upload Document

curl -H "X-API-Key: dev-key-1" \
-F "file=@data/sample.pdf" \
http://localhost:8080/api/v1/documents

---

### 4. Check Task

curl -H "X-API-Key: dev-key-1" \
http://localhost:8080/api/v1/tasks/{taskId}

---

## Technologies Used

Backend:
- Java 21
- Spring Boot 3
- Spring Data JPA
- H2 Database

AI Service:
- Python 3.12
- FastAPI
- ChromaDB
- Ollama
- httpx

Deployment:
- Docker
- Docker Compose

Models:
- llama3.2:3b
- nomic-embed-text

---

## Enterprise Architecture Features

- Microservice separation
- Async task processing
- Persistent task state tracking
- Secure authentication
- Vector database integration
- Grounded AI responses
- Fully self‑hosted deployment

---

## Future Improvements

- Retry logic
- Monitoring integration
- PostgreSQL support
- Horizontal scaling
- Distributed task queue

---

## Author

Enterprise RAG Backend System
