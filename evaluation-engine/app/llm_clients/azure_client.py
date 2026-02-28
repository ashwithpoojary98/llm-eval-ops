"""
Azure OpenAI LLM client implementation.
"""
import time
from typing import Optional
import structlog

from openai import AzureOpenAI
from tenacity import retry, stop_after_attempt, wait_exponential

from app.llm_clients.base import BaseLLMClient, LLMResponse
from app.config import get_settings

logger = structlog.get_logger()
settings = get_settings()


class AzureOpenAIClient(BaseLLMClient):
    """Azure OpenAI API client."""

    def __init__(
        self,
        api_key: str,
        model: str,  # This is the deployment name in Azure
        api_url: str,  # Azure endpoint URL (required)
        **config,
    ):
        super().__init__(api_key, model, api_url, **config)

        if not api_url:
            raise ValueError("Azure OpenAI requires api_url (endpoint URL)")

        # Azure-specific configuration
        api_version = config.get("api_version", "2024-02-15-preview")

        self.client = AzureOpenAI(
            api_key=api_key,
            api_version=api_version,
            azure_endpoint=api_url,
            timeout=self.timeout,
        )

        self.deployment_name = model  # In Azure, model == deployment name
        self.system_message = config.get(
            "system_message",
            "You are a helpful assistant that evaluates AI responses."
        )

    @retry(
        stop=stop_after_attempt(settings.retry_attempts),
        wait=wait_exponential(multiplier=settings.retry_delay, min=1, max=10),
    )
    def generate(self, prompt: str, **kwargs) -> str:
        """Generate text using Azure OpenAI API."""
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
                model=self.deployment_name,
                messages=messages,
                temperature=kwargs.get("temperature", self.temperature),
                max_tokens=kwargs.get("max_tokens", self.max_tokens),
            )

            latency_ms = int((time.time() - start_time) * 1000)

            content = response.choices[0].message.content or ""
            input_tokens = response.usage.prompt_tokens if response.usage else 0
            output_tokens = response.usage.completion_tokens if response.usage else 0

            return LLMResponse(
                content=content,
                input_tokens=input_tokens,
                output_tokens=output_tokens,
                total_tokens=input_tokens + output_tokens,
                latency_ms=latency_ms,
                model=f"azure:{self.deployment_name}",
                metadata={
                    "finish_reason": response.choices[0].finish_reason,
                    "deployment": self.deployment_name,
                },
            )

        except Exception as e:
            logger.error(
                "Azure OpenAI API error",
                error=str(e),
                deployment=self.deployment_name,
            )
            raise
