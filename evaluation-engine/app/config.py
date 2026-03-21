"""
Application configuration using Pydantic Settings.
Loads from environment variables or .env file.
"""
from functools import lru_cache
from typing import Optional
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
    )

    # Application
    app_name: str = "LLMOps Evaluation Engine"
    app_version: str = "0.1.0"
    debug: bool = False

    # Server
    host: str = "0.0.0.0"
    port: int = 8000

    # Database
    database_url: str = "postgresql+asyncpg://postgres:postgres@localhost:5432/llmops_eval"
    db_pool_size: int = 10
    db_max_overflow: int = 20

    # Spring Boot Backend (for callbacks)
    spring_boot_url: str = "http://localhost:8080"
    spring_boot_api_key: Optional[str] = None
    # Shared secret sent in X-Callback-Secret header — must match EVALUATION_CALLBACK_SECRET in Spring Boot
    callback_secret: Optional[str] = None

    # Default LLM Settings
    default_temperature: float = 0.7
    default_max_tokens: int = 1000
    default_timeout: int = 60

    # Evaluation Settings
    max_parallel_evaluations: int = 5
    batch_size: int = 10
    retry_attempts: int = 3
    retry_delay: float = 1.0

    # Metrics Settings
    embedding_model: str = "all-MiniLM-L6-v2"
    bertscore_model: str = "microsoft/deberta-xlarge-mnli"

    # CORS
    cors_origins: list[str] = ["http://localhost:3000", "http://localhost:8080"]


@lru_cache
def get_settings() -> Settings:
    """Get cached settings instance."""
    return Settings()
