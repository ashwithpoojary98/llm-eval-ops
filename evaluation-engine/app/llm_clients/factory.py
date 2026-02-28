"""
LLM Client factory - creates appropriate client based on provider type.
"""
from typing import Optional
import structlog

from app.models.enums import ProviderType
from app.models.requests import LLMEndpointConfig
from app.llm_clients.base import BaseLLMClient
from app.llm_clients.openai_client import OpenAIClient
from app.llm_clients.anthropic_client import AnthropicClient
from app.llm_clients.azure_client import AzureOpenAIClient
from app.llm_clients.custom_client import CustomLLMClient

logger = structlog.get_logger()


class LLMClientFactory:
    """Factory for creating LLM clients based on provider type."""

    _client_classes: dict[ProviderType, type] = {
        ProviderType.OPENAI: OpenAIClient,
        ProviderType.ANTHROPIC: AnthropicClient,
        ProviderType.AZURE_OPENAI: AzureOpenAIClient,
        ProviderType.CUSTOM: CustomLLMClient,
    }

    @classmethod
    def create(cls, config: LLMEndpointConfig) -> BaseLLMClient:
        """
        Create an LLM client from configuration.

        Args:
            config: LLM endpoint configuration

        Returns:
            Configured LLM client instance
        """
        client_class = cls._client_classes.get(config.provider_type)

        if not client_class:
            # Fall back to custom client for unknown providers
            logger.warning(
                "Unknown provider type, using custom client",
                provider=config.provider_type,
            )
            client_class = CustomLLMClient

        # Build kwargs from config
        kwargs = config.config or {}

        return client_class(
            api_key=config.api_key,
            model=config.model_name,
            api_url=config.api_url,
            **kwargs,
        )

    @classmethod
    def get_supported_providers(cls) -> list[ProviderType]:
        """Get list of supported provider types."""
        return list(cls._client_classes.keys())


def create_llm_client(config: LLMEndpointConfig) -> BaseLLMClient:
    """
    Convenience function to create an LLM client.

    Args:
        config: LLM endpoint configuration

    Returns:
        Configured LLM client instance
    """
    return LLMClientFactory.create(config)
