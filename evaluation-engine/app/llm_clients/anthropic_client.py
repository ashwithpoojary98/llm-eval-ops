"""
Anthropic (Claude) LLM client implementation.
"""
import time
from typing import Optional
import structlog

from anthropic import Anthropic
from tenacity import retry, stop_after_attempt, wait_exponential

from app.llm_clients.base import BaseLLMClient, LLMResponse
from app.config import get_settings

logger = structlog.get_logger()
settings = get_settings()

# Pricing per 1K tokens (approximate)
ANTHROPIC_PRICING = {
    "claude-3-opus-20240229": {"input": 0.015, "output": 0.075},
    "claude-3-sonnet-20240229": {"input": 0.003, "output": 0.015},
    "claude-3-haiku-20240307": {"input": 0.00025, "output": 0.00125},
    "claude-2.1": {"input": 0.008, "output": 0.024},
    "claude-2.0": {"input": 0.008, "output": 0.024},
}


class AnthropicClient(BaseLLMClient):
    """Anthropic Claude API client."""

    def __init__(
        self,
        api_key: str,
        model: str = "claude-3-sonnet-20240229",
        api_url: Optional[str] = None,
        **config,
    ):
        super().__init__(api_key, model, api_url, **config)

        self.client = Anthropic(
            api_key=api_key,
            base_url=api_url,
            timeout=self.timeout,
        )

        self.system_message = config.get(
            "system_message",
            "You are a helpful assistant that evaluates AI responses."
        )

    @retry(
        stop=stop_after_attempt(settings.retry_attempts),
        wait=wait_exponential(multiplier=settings.retry_delay, min=1, max=10),
    )
    def generate(self, prompt: str, **kwargs) -> str:
        """Generate text using Anthropic API."""
        response = self.generate_with_metadata(prompt, **kwargs)
        return response.content

    @retry(
        stop=stop_after_attempt(settings.retry_attempts),
        wait=wait_exponential(multiplier=settings.retry_delay, min=1, max=10),
    )
    def generate_with_metadata(self, prompt: str, **kwargs) -> LLMResponse:
        """Generate text with full metadata."""
        start_time = time.time()

        try:
            response = self.client.messages.create(
                model=self.model,
                max_tokens=kwargs.get("max_tokens", self.max_tokens),
                system=self.system_message,
                messages=[
                    {"role": "user", "content": prompt}
                ],
                temperature=kwargs.get("temperature", self.temperature),
            )

            latency_ms = int((time.time() - start_time) * 1000)

            # Extract content from response
            content = ""
            if response.content:
                content = response.content[0].text if response.content else ""

            input_tokens = response.usage.input_tokens if response.usage else 0
            output_tokens = response.usage.output_tokens if response.usage else 0

            # Calculate cost
            pricing = ANTHROPIC_PRICING.get(self.model, {"input": 0.003, "output": 0.015})
            cost = (input_tokens / 1000) * pricing["input"] + \
                   (output_tokens / 1000) * pricing["output"]

            return LLMResponse(
                content=content,
                input_tokens=input_tokens,
                output_tokens=output_tokens,
                total_tokens=input_tokens + output_tokens,
                latency_ms=latency_ms,
                model=self.model,
                metadata={
                    "stop_reason": response.stop_reason,
                    "cost_usd": cost,
                },
            )

        except Exception as e:
            logger.error("Anthropic API error", error=str(e), model=self.model)
            raise
