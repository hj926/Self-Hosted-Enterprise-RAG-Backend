项目实现了什么
**核心目标**
- **完全本地**跑通 RAG 全链路（不依赖云服务/不需要付费 API）。
- 提供**后端工程化**能力：鉴权、多租户隔离、异步任务、文档管理。
- 数据落盘持久化，并且可以通过脚本与 curl **可重复验证**。
**关键能力**
- Docker Compose 多服务架构：
  - `backend-api`（Java Spring Boot，端口 **8080**）
  - `rag-service`（Python FastAPI，端口 **8000**）
  - `ollama`（本地 LLM/Embedding，端口 **11434**）
- **API Key 鉴权 + 多租户隔离**
  - 必带请求头：`X-API-Key` + `X-Tenant-ID`
  - 不同 key 映射到不同 tenant（tenantA/tenantB）。
- **异步 ingest 任务链路**
  - 上传 PDF → 创建任务 → Worker 调用 rag-service → 轮询 → 更新任务状态
  - 任务信息落在 **H2 文件数据库**（持久化）。
- **文档管理 API（以 DB 为准）**
  - `GET /api/v1/documents`：从 DB 列出 READY 文档
  - `GET /api/v1/documents/{docId}`：DB 元数据 + 尝试从 RAG 拿 `chunkCount`
  - `DELETE /api/v1/documents/{docId}`：DB 软删除 + 尝试删除 RAG 向量
- **严格检索模式**
  - rag-service 可配置严格模式（`STRICT_RAG=true`），强调“有证据再回答”，并输出 citations。
技术栈
- **Java 后端**：Spring Boot / Web / JPA / H2（文件模式）
- **Python RAG 微服务**：FastAPI / Pydantic / ChromaDB（本地）/ PDF ingest+切块+检索
- **LLM/Embedding**：Ollama（启动时自动 pull 默认模型）
- **基础设施**：Docker Compose / Makefile / 自动 smoke test 脚本
架构说明
请求链路（成功路径）：
1) 客户端上传 PDF 到 **backend-api**  
2) 后端创建 **ingest 任务**（写入 H2）  
3) Worker 将 PDF 发给 **rag-service**  
4) rag-service 切块/向量化/写入向量库（按 tenant 落盘）  
5) Worker 将任务置为 `SUCCEEDED`，并将文档元数据写入 `documents` 表  
6) 用户通过 backend 发起 query，backend 转发给 rag-service，返回 **答案 + 引用**
项目目录结构（高层）
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
持久化目录说明
如何运行
前置条件
启动（推荐）
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
健康检查
```bash
curl -s http://127.0.0.1:8080/api/v1/health -H "X-API-Key: dev-key-1" | python -m json.tool
```
```bash
curl -s http://127.0.0.1:8000/health | python -m json.tool
```
```bash
curl -s http://127.0.0.1:11434/api/tags | python -m json.tool
```
配置说明
后端配置
RAG 微服务配置
API 使用与验证
上传 PDF → taskId
```bash
curl -s -X POST "http://127.0.0.1:8080/api/v1/documents" \
  -H "X-API-Key: dev-key-1" \
  -H "X-Tenant-ID: tenantA" \
  -F "file=@./data/sample.pdf;type=application/pdf" | python -m json.tool
```
```json
{ "taskId": "..." }
```
轮询任务状态
```bash
TASK_ID="paste_task_id_here"
curl -s "http://127.0.0.1:8080/api/v1/tasks/$TASK_ID" \
  -H "X-API-Key: dev-key-1" \
  -H "X-Tenant-ID: tenantA" | python -m json.tool
```
列出文档（以 DB 为准）
```bash
curl -s "http://127.0.0.1:8080/api/v1/documents?page=0&size=20" \
  -H "X-API-Key: dev-key-1" \
  -H "X-Tenant-ID: tenantA" | python -m json.tool
```
文档详情
```bash
DOC_ID="paste_doc_id_here"
curl -s "http://127.0.0.1:8080/api/v1/documents/$DOC_ID" \
  -H "X-API-Key: dev-key-1" \
  -H "X-Tenant-ID: tenantA" | python -m json.tool
```
查询（答案 + 引用）
```bash
curl -s -X POST "http://127.0.0.1:8080/api/v1/query" \
  -H "X-API-Key: dev-key-1" \
  -H "X-Tenant-ID: tenantA" \
  -H "Content-Type: application/json" \
  -d '{"question":"What is this document about? Answer briefly and cite sources.","top_k":3}' \
  | python -m json.tool
```
删除（DB 软删 + RAG 向量删除）
```bash
DOC_ID="paste_doc_id_here"
curl -i -X DELETE "http://127.0.0.1:8080/api/v1/documents/$DOC_ID" \
  -H "X-API-Key: dev-key-1" \
  -H "X-Tenant-ID: tenantA"
```
验证 rag-service 侧是否真的删除
```bash
curl -i "http://127.0.0.1:8000/documents/$DOC_ID" -H "X-Tenant-ID: tenantA"
```
```bash
curl -i "http://127.0.0.1:8000/documents/$DOC_ID" -H "X-Tenant-ID: tenantA"
```
租户隔离验证
```bash
curl -i "http://127.0.0.1:8080/api/v1/documents/$DOC_ID" \
  -H "X-API-Key: dev-key-2" \
  -H "X-Tenant-ID: tenantB"
```
自动化验证（推荐）
```bash
make verify
```
```bash
./scripts/smoke_test.sh
```
```bash
BASE_URL=http://127.0.0.1:8080 API_KEY=dev-key-1 PDF_PATH=data/sample.pdf ./scripts/smoke_test.sh
```
可观测性与排错
查看日志
```bash
docker logs -f --tail=200 infra-backend-api-1
docker logs -f --tail=200 infra-rag-service-1
docker logs -f --tail=200 infra-ollama-1
```
常见错误与解决
数据存在哪里？
可扩展方向（建议）
portfolio use.
