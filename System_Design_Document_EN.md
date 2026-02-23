
# Self-Hosted Enterprise RAG Backend System
# System Design Document

---

# 1. Executive Summary

This document describes the system design of a self-hosted, enterprise-grade Retrieval-Augmented Generation (RAG) backend platform. The system integrates a Java Spring Boot backend API, a Python FastAPI-based RAG microservice, and Ollama-powered local large language models and embedding models.

The primary design goals are:

- Fully local execution without any cloud dependencies
- Multi-tenant isolation with secure API key authentication
- Asynchronous document ingestion pipeline
- Persistent storage for both metadata and vector embeddings
- Backend-authoritative document lifecycle management
- Modular, production-grade architecture with observability

The system enables users to upload PDF documents, ingest them into a vector database, and query grounded answers with source citations.

---

# 2. High-Level Architecture

## Core Components

| Component | Technology | Responsibility |
|---------|------------|----------------|
| Backend API | Spring Boot | Authentication, task management, document metadata management |
| RAG Service | FastAPI | Document ingestion, chunking, embedding, retrieval |
| Vector Store | ChromaDB | Persistent vector storage |
| LLM / Embeddings | Ollama | Embedding generation and answer synthesis |
| Database | H2 | Persistent task and document metadata |
| Storage | Local filesystem | Tenant-isolated persistent storage |

## Logical Request Flow

Client  
→ Backend API (Spring Boot)  
→ Ingestion Worker  
→ RAG Service (FastAPI)  
→ Ollama (LLM and embeddings)  
→ ChromaDB  

This architecture separates control-plane logic (backend API) from compute-plane logic (RAG service), ensuring scalability and maintainability.

---

# 3. Core Design Principles

## 3.1 Backend-Authoritative Design

Document metadata is stored and managed exclusively in the backend database, which serves as the single source of truth.

The backend is responsible for:

- Document lifecycle management
- Task orchestration and state tracking
- Tenant isolation enforcement

The RAG service functions purely as a compute engine and does not control document authority.

---

## 3.2 Multi-Tenant Isolation

Tenant isolation is enforced using multiple mechanisms:

- API key authentication
- Tenant ID request headers
- Per-tenant storage directories
- Per-tenant registry and vector namespace

This guarantees strict data isolation across tenants.

---

## 3.3 Asynchronous Ingestion Pipeline

The ingestion workflow follows this pipeline:

Upload → Task creation → Worker processing → Vector storage → Metadata persistence

Benefits include:

- Non-blocking API performance
- Improved scalability
- Failure isolation and recovery capability

---

## 3.4 Soft Delete Model

Document deletion follows a soft-delete strategy:

- Document is marked deleted in the backend database
- Vector data is deleted in the RAG service (best-effort)
- Access to deleted documents is prevented

This enables auditability and safer lifecycle management.

---

# 4. Data Model Design

## Document Entity

Fields include:

- docId
- tenantId
- filename
- status
- createdAt
- deletedAt
- lastIngestTaskId

## Task Entity

Fields include:

- taskId
- tenantId
- status
- createdAt
- updatedAt
- docId

---

# 5. Request Flow

## Ingestion Flow

Client → Backend API  
Backend → Create ingestion task  
Worker → Send document to RAG service  
RAG Service → Chunk, embed, and store vectors  
Worker → Update backend database metadata  

---

## Query Flow

Client → Backend API  
Backend → Forward query to RAG service  
RAG Service → Retrieve relevant chunks and generate grounded answer  
Backend → Return answer and citations to client  

---

# 6. Storage Design

Persistent storage locations:

Backend database:

storage/h2/

Vector storage:

storage/chroma/

Tenant-specific storage:

storage/tenants/<tenantId>/

---

# 7. Failure Handling

The system handles the following failure scenarios:

- Authentication failures
- Missing or invalid files
- Invalid or malformed requests
- Internal processing errors

All failures are logged for debugging and observability.

---

# 8. Security Design

Security mechanisms include:

- API key authentication
- Strict tenant isolation
- Controlled backend-managed document lifecycle

These mechanisms ensure secure and isolated access to system resources.

---

# 9. Scalability Considerations

The architecture is designed for future scalability:

- Replace H2 with PostgreSQL or another production database
- Horizontally scale backend API instances
- Scale RAG service independently
- Support distributed vector storage systems

---

# 10. Design Tradeoffs

Key design decision: Backend-authoritative architecture

Benefits:

- Strong consistency guarantees
- Clear lifecycle ownership
- Simplified system coordination

Tradeoff:

- Increased backend responsibility and coordination complexity

---

# 11. Deployment Model

The system is deployed using Docker Compose.

Containers include:

- backend-api
- rag-service
- ollama

This enables reproducible and isolated deployment.

---

# 12. Observability

Observability is provided through container logs:

docker logs backend-api  
docker logs rag-service  

This allows debugging and system monitoring.

---

# 13. Conclusion

This system implements a production-grade, self-hosted RAG backend platform with:

- Multi-tenant isolation
- Asynchronous ingestion pipeline
- Persistent storage architecture
- Backend-authoritative lifecycle management
- Modular microservice architecture

This system is suitable for:

- Enterprise internal deployment
- Backend engineering portfolio projects
- System design demonstrations
- AI backend infrastructure learning and experimentation

