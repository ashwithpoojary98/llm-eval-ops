"""
Base class for LLM clients.
"""
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Optional, Any


@dataclass
class LLMResponse:
    """Response from an LLM call."""

    content: str
    input_tokens: int = 0
    output_tokens: int = 0
    total_tokens: int = 0
    latency_ms: int = 0
    model: str = ""
    metadata: dict[str, Any] = field(default_factory=dict)

    @property
    def cost_usd(self) -> float:
        """Estimate cost based on typical pricing. Override in subclass for accuracy."""
        # Default rough estimate: $0.01 per 1K tokens (varies by model)
        return (self.total_tokens / 1000) * 0.01


class BaseLLMClient(ABC):
    """Abstract base class for LLM clients."""

    def __init__(
        self,
        api_key: str,
        model: str,
        api_url: Optional[str] = None,
        **config,
    ):
        self.api_key = api_key
        self.model = model
        self.api_url = api_url
        self.config = config

        # Default configuration
        self.temperature = config.get("temperature", 0.0)  # Deterministic for evaluation
        self.max_tokens = config.get("max_tokens", 1000)
        self.timeout = config.get("timeout", 60)

    @abstractmethod
    def generate(self, prompt: str, **kwargs) -> str:
        """
        Generate a response from the LLM.

        Args:
            prompt: The input prompt
            **kwargs: Additional generation parameters

        Returns:
            Generated text content
        """
        pass

    @abstractmethod
    def generate_with_metadata(self, prompt: str, **kwargs) -> LLMResponse:
        """
        Generate a response with full metadata.

        Args:
            prompt: The input prompt
            **kwargs: Additional generation parameters

        Returns:
            LLMResponse with content, tokens, and metadata
        """
        pass

    def estimate_tokens(self, text: str) -> int:
        """
        Estimate token count for text.
        Simple approximation: ~4 characters per token.
        Override for accurate counting.
        """
        return len(text) // 4
