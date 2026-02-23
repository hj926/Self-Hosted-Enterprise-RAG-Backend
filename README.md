# Self-Hosted Enterprise RAG Backend System (Spring Boot + FastAPI + Ollama)

> This repository implements a fully local, multi-service Retrieval-Augmented Generation (RAG) backend with a production-style Java API gateway and a Python RAG microservice.  

---

## 1. What you built

**Core goals**
- Run RAG end-to-end **locally** (no paid cloud services required).
- Provide a **backend-grade API** with authentication, multi-tenant isolation, async tasks, and document management.
- Keep storage **persistent** on disk and make the system verifiable with scripts + curl.

**Key features**
- **Multi-service architecture** via Docker Compose:
  - `backend-api` (Java Spring Boot, port **8080**)
  - `rag-service` (Python FastAPI, port **8000**)
  - `ollama` (LLM/embeddings, port **11434**)
- **API-key authentication + tenant isolation**
  - Headers: `X-API-Key` + `X-Tenant-ID`
  - Different keys map to different tenants (tenantA/tenantB).
- **Async ingest task pipeline**
  - Upload PDF → create ingest task → worker calls RAG service → poll → update task status.
  - Task tracking stored in **H2** (file-based, persistent).
- **Document Management API (DB authoritative)**
  - `GET /api/v1/documents` lists **READY** documents from DB.
  - `GET /api/v1/documents/{docId}` returns DB metadata (+ best-effort `chunkCount` from RAG).
  - `DELETE /api/v1/documents/{docId}` soft-deletes in DB and best-effort deletes vectors from RAG.
- **Strict RAG mode**
  - RAG service can be configured to answer with citations and avoid hallucinations when context is missing (`STRICT_RAG=true`).

## 2. Tech stack
- **Java Backend**: Spring Boot, Spring Web, Spring Data JPA, H2 (file mode)
- **Python RAG Service**: FastAPI, Pydantic, ChromaDB (local), PDF ingest + chunking + retrieval
- **LLM/Embeddings**: Ollama (default models pulled on startup)
- **Infra**: Docker Compose, Makefile, smoke test script

## 3. Architecture
Request flow (happy path):

1) Client uploads PDF to **backend-api**  
2) backend creates an **ingest task** (persisted in H2)  
3) worker sends PDF to **rag-service**  
4) rag-service chunks + embeds + stores vectors (per-tenant storage)  
5) worker marks task `SUCCEEDED` and **upserts document metadata** into `documents` table  
6) user queries via backend; backend forwards to rag-service and returns **answer + citations**

## 4. Project layout
```text
Self-Hosted-Enterprise-RAG-Backend/
  backend-java/                 # Spring Boot backend
    src/main/...                # controllers, services, worker, etc.
    src/main/resources/application.yml
  src/                          # Python rag-service package
    api/                        # FastAPI routes (health, documents, query)
    rag/                        # core RAG engine, clients, storage
  infra/                        # Dockerfiles + docker-compose.yml
  scripts/                      # smoke_test.sh (end-to-end verification)
  data/                         # sample.pdf and other inputs
  storage/                      # persistent data (H2 + per-tenant RAG storage)
  Makefile
  requirements.txt
  pytest.ini
  tests/
```
### Storage
## 5. How to run
### Prerequisites
### Start (recommended)
```bash
make up
```
```bash
make down
```
```bash
make nuke
```
```bash
make logs
```
### Health checks
```bash
curl -s http://127.0.0.1:8080/api/v1/health -H "X-API-Key: dev-key-1" | python -m json.tool
```
```bash
curl -s http://127.0.0.1:8000/health | python -m json.tool
```
```bash
curl -s http://127.0.0.1:11434/api/tags | python -m json.tool
```
## 6. Configuration
### Backend configuration (Spring Boot)
### RAG service configuration
## 7. API usage & verification
### 7.1 Upload PDF → taskId
```bash
curl -s -X POST "http://127.0.0.1:8080/api/v1/documents" \
  -H "X-API-Key: dev-key-1" \
  -H "X-Tenant-ID: tenantA" \
  -F "file=@./data/sample.pdf;type=application/pdf" | python -m json.tool
```
```json
{ "taskId": "..." }
```
### 7.2 Poll task → SUCCEEDED + docId
```bash
TASK_ID="paste_task_id_here"
curl -s "http://127.0.0.1:8080/api/v1/tasks/$TASK_ID" \
  -H "X-API-Key: dev-key-1" \
  -H "X-Tenant-ID: tenantA" | python -m json.tool
```
### 7.3 List documents (DB authoritative)
```bash
curl -s "http://127.0.0.1:8080/api/v1/documents?page=0&size=20" \
  -H "X-API-Key: dev-key-1" \
  -H "X-Tenant-ID: tenantA" | python -m json.tool
```
### 7.4 Document detail
```bash
DOC_ID="paste_doc_id_here"
curl -s "http://127.0.0.1:8080/api/v1/documents/$DOC_ID" \
  -H "X-API-Key: dev-key-1" \
  -H "X-Tenant-ID: tenantA" | python -m json.tool
```
### 7.5 Query (answer + citations)
```bash
curl -s -X POST "http://127.0.0.1:8080/api/v1/query" \
  -H "X-API-Key: dev-key-1" \
  -H "X-Tenant-ID: tenantA" \
  -H "Content-Type: application/json" \
  -d '{"question":"What is this document about? Answer briefly and cite sources.","top_k":3}' \
  | python -m json.tool
```
### 7.6 Delete (soft delete in DB + delete vectors in RAG)
```bash
DOC_ID="paste_doc_id_here"
curl -i -X DELETE "http://127.0.0.1:8080/api/v1/documents/$DOC_ID" \
  -H "X-API-Key: dev-key-1" \
  -H "X-Tenant-ID: tenantA"
```
### 7.7 Verify RAG-side deletion
```bash
curl -i "http://127.0.0.1:8000/documents/$DOC_ID" -H "X-Tenant-ID: tenantA"
```
```bash
curl -i "http://127.0.0.1:8000/documents/$DOC_ID" -H "X-Tenant-ID: tenantA"
```
### 7.8 Tenant isolation check
```bash
curl -i "http://127.0.0.1:8080/api/v1/documents/$DOC_ID" \
  -H "X-API-Key: dev-key-2" \
  -H "X-Tenant-ID: tenantB"
```
## 8. Automated verification
```bash
make verify
```
```bash
./scripts/smoke_test.sh
```
```bash
BASE_URL=http://127.0.0.1:8080 API_KEY=dev-key-1 PDF_PATH=data/sample.pdf ./scripts/smoke_test.sh
```
## 9. Observability & troubleshooting
### Logs
```bash
docker logs -f --tail=200 infra-backend-api-1
docker logs -f --tail=200 infra-rag-service-1
docker logs -f --tail=200 infra-ollama-1
```
### Common error patterns
### Where is data stored?
## 10. Roadmap ideas
For educational
