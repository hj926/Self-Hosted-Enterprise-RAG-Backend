
# 自托管企业级 RAG 后端系统（Spring Boot + FastAPI + Ollama）

> **双语 README / 中文版本**  
> 本仓库实现了一个完全本地运行的、多服务架构的检索增强生成（RAG）后端系统，包含生产级 Java API 网关和 Python RAG 微服务。

---

## 1. 本项目实现了什么

**核心目标**
- 在本地端到端运行 RAG（无需任何付费云服务）。
- 提供具备生产级能力的后端 API，包括认证、多租户隔离、异步任务和文档管理。
- 将数据持久化存储在磁盘，并支持通过脚本 + curl 进行完整验证。

**关键功能**
- 通过 Docker Compose 实现多服务架构：
  - `backend-api`（Java Spring Boot，端口 **8080**）
  - `rag-service`（Python FastAPI，端口 **8000**）
  - `ollama`（LLM/embedding 服务，端口 **11434**）

- API Key 认证 + 多租户隔离
  - 请求头：`X-API-Key` 和 `X-Tenant-ID`
  - 不同 key 映射到不同 tenant（tenantA / tenantB）。

- 异步 ingest 任务流水线
  - 上传 PDF → 创建 ingest task → worker 调用 RAG 服务 → 轮询任务状态 → 更新任务状态。
  - 任务状态存储在 H2 数据库（基于文件，持久化）。

- 文档管理 API（数据库为权威数据源）
  - `GET /api/v1/documents` 从数据库列出 READY 状态文档。
  - `GET /api/v1/documents/{docId}` 返回数据库元数据（+ 尝试从 RAG 获取 chunkCount）。
  - `DELETE /api/v1/documents/{docId}` 在数据库中软删除，并尝试从 RAG 删除向量。

- 严格 RAG 模式
  - 当上下文缺失时，RAG 服务可以配置为只返回带引用的答案并避免幻觉（`STRICT_RAG=true`）。

---

## 2. 技术栈

- Java 后端：Spring Boot, Spring Web, Spring Data JPA, H2（文件模式）
- Python RAG 服务：FastAPI, Pydantic, ChromaDB（本地）, PDF ingest + chunking + retrieval
- LLM / Embeddings：Ollama（默认模型启动时自动下载）
- 基础设施：Docker Compose, Makefile, smoke test 脚本

---

## 3. 系统架构

请求流程（正常流程）：

1）客户端上传 PDF 到 backend-api  
2）backend 创建 ingest task（持久化到 H2）  
3）worker 将 PDF 发送到 rag-service  
4）rag-service 执行 chunk + embedding + 存储向量（按 tenant 隔离存储）  
5）worker 标记任务为 SUCCEEDED，并将文档元数据写入 documents 表  
6）用户通过 backend 查询；backend 转发到 rag-service 并返回答案 + 引用  

---

## 4. 项目结构

Self-Hosted-Enterprise-RAG-Backend/

  backend-java/                 # Spring Boot 后端  
    src/main/...                # controller, service, worker 等  
    src/main/resources/application.yml  

  src/                          # Python rag-service  
    api/                        # FastAPI 路由（health, documents, query）  
    rag/                        # RAG 核心引擎  

  infra/                        # Dockerfile + docker-compose.yml  
  scripts/                      # smoke_test.sh 验证脚本  
  data/                         # 示例 PDF  
  storage/                      # 持久化存储  
  Makefile  
  requirements.txt  
  tests/  

---

## Storage 存储

- Backend H2 数据库：storage/h2/  
- RAG tenant 存储：storage/tenants/<tenantId>/  
- Chroma 向量存储：storage/chroma/  

---

## 5. 如何运行

前置条件：

- Docker Desktop 或 Docker Engine
- Docker Compose

启动：

make up

停止：

make down

删除数据并停止：

make nuke

查看日志：

make logs

---

## 健康检查

Backend：

curl -s http://127.0.0.1:8080/api/v1/health -H "X-API-Key: dev-key-1"

RAG：

curl -s http://127.0.0.1:8000/health

Ollama：

curl -s http://127.0.0.1:11434/api/tags

---

## 6. 配置

Backend 配置：

backend-java/src/main/resources/application.yml

关键参数：

backend.ragBaseUrl  
backend.apiKeyHeader  
backend.tenantHeader  
backend.auth.keys  

默认 key：

dev-key-1 → tenantA  
dev-key-2 → tenantB  

RAG 配置：

infra/docker-compose.yml

OLLAMA_BASE_URL  
STRICT_RAG  
RAG_CHROMA_DIR  
RAG_REGISTRY_PATH  

---

## 7. API 使用

上传 PDF：

POST /api/v1/documents

返回：

taskId

查询任务：

GET /api/v1/tasks/{taskId}

返回：

status  
docId  

查询文档：

GET /api/v1/documents

查询文档详情：

GET /api/v1/documents/{docId}

查询 RAG：

POST /api/v1/query

返回：

answer  
citations  
retrieved_count  

删除文档：

DELETE /api/v1/documents/{docId}

---

## 8. 自动验证

make verify

或

scripts/smoke_test.sh

---

## 9. 日志和故障排查

查看日志：

docker logs infra-backend-api-1  
docker logs infra-rag-service-1  
docker logs infra-ollama-1  

常见错误：

400 BAD_REQUEST  
401 UNAUTHORIZED  
404 NOT_FOUND  
405 METHOD_NOT_ALLOWED  

---

## 10. Roadmap

未来优化方向：

- 文档分页
- 硬删除支持
- RBAC 权限控制
- 限流和监控
- ingestion 去重
- 流式响应

---

## License

教育和作品集用途。
