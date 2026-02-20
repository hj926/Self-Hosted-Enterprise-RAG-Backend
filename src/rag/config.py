from __future__ import annotations

from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field


class RagSettings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    app_env: str = Field(default="dev", alias="APP_ENV")
    log_level: str = Field(default="INFO", alias="LOG_LEVEL")

    rag_chroma_dir: str = Field(default="storage/chroma", alias="RAG_CHROMA_DIR")
    rag_registry_path: str = Field(
        default="storage/registry.json", alias="RAG_REGISTRY_PATH"
    )

    ollama_base_url: str = Field(
        default="http://localhost:11434", alias="OLLAMA_BASE_URL"
    )
    ollama_llm_model: str = Field(default="llama3.2:3b", alias="OLLAMA_LLM_MODEL")
    ollama_embed_model: str = Field(
        default="nomic-embed-text", alias="OLLAMA_EMBED_MODEL"
    )

    rag_top_k: int = Field(default=6, alias="RAG_TOP_K")
    rag_mmr_lambda: float = Field(default=0.5, alias="RAG_MMR_LAMBDA")
    rag_chunk_size: int = Field(default=800, alias="RAG_CHUNK_SIZE")
    rag_chunk_overlap: int = Field(default=120, alias="RAG_CHUNK_OVERLAP")

    llm_temperature: float = Field(default=0.2, alias="LLM_TEMPERATURE")
    llm_max_tokens: int = Field(default=512, alias="LLM_MAX_TOKENS")

    http_timeout_seconds: int = Field(default=60, alias="HTTP_TIMEOUT_SECONDS")
    strict_rag: bool = Field(default=True, alias="STRICT_RAG")
