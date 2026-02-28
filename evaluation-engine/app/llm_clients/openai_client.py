"""
OpenAI LLM client implementation.
"""
import time
from typing import Optional
import structlog

from openai import OpenAI
from tenacity import retry, stop_after_attempt, wait_exponential

from app.llm_clients.base import BaseLLMClient, LLMResponse
from app.config import get_settings

logger = structlog.get_logger()
settings = get_settings()

# Pricing per 1K tokens (as of early 2024, approximate)
OPENAI_PRICING = {
    "gpt-4-turbo": {"input": 0.01, "output": 0.03},
    "gpt-4-turbo-preview": {"input": 0.01, "output": 0.03},
    "gpt-4": {"input": 0.03, "output": 0.06},
    "gpt-4-32k": {"input": 0.06, "output": 0.12},
    "gpt-3.5-turbo": {"input": 0.0005, "output": 0.0015},
    "gpt-3.5-turbo-16k": {"input": 0.003, "output": 0.004},
}


class OpenAIClient(BaseLLMClient):
    """OpenAI API client."""

    def __init__(
        self,
        api_key: str,
        model: str = "gpt-4-turbo",
        api_url: Optional[str] = None,
        **config,
    ):
        super().__init__(api_key, model, api_url, **config)

        self.client = OpenAI(
            api_key=api_key,
            base_url=api_url,  # For custom endpoints or Azure
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
        """Generate text using OpenAI API."""
        response = self.generate_with_metadata(prompt, **kwargs)
        return response.content

    @retry(
        stop=stop_after_attempt(settings.retry_attempts),
        wait=wait_exponential(multiplier=settings.retry_delay, min=1, max=10),
    )
    def generate_with_metadata(self, prompt: str, **kwargs) -> LLMResponse:
        """Generate text with full metadata."""
        start_time = time.time()

        messages = [
            {"role": "system", "content": self.system_message},
            {"role": "user", "content": prompt},
        ]

        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                temperature=kwargs.get("temperature", self.temperature),
                max_tokens=kwargs.get("max_tokens", self.max_tokens),
            )

            latency_ms = int((time.time() - start_time) * 1000)

            content = response.choices[0].message.content or ""
            input_tokens = response.usage.prompt_tokens if response.usage else 0
            output_tokens = response.usage.completion_tokens if response.usage else 0

            # Calculate cost
            pricing = OPENAI_PRICING.get(self.model, {"input": 0.01, "output": 0.03})
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
                    "finish_reason": response.choices[0].finish_reason,
                    "cost_usd": cost,
                },
            )

        except Exception as e:
            logger.error("OpenAI API error", error=str(e), model=self.model)
            raise

    def estimate_tokens(self, text: str) -> int:
        """Estimate tokens using tiktoken if available."""
        try:
            import tiktoken
            encoding = tiktoken.encoding_for_model(self.model)
            return len(encoding.encode(text))
        except Exception:
            return super().estimate_tokens(text)
