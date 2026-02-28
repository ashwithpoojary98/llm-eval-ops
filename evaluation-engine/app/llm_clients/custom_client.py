"""
Custom/Generic LLM client for OpenAI-compatible APIs.
"""
import time
from typing import Optional
import structlog
import httpx

from tenacity import retry, stop_after_attempt, wait_exponential

from app.llm_clients.base import BaseLLMClient, LLMResponse
from app.config import get_settings

logger = structlog.get_logger()
settings = get_settings()


class CustomLLMClient(BaseLLMClient):
    """
    Generic LLM client for OpenAI-compatible APIs.

    Works with:
    - Local LLMs (Ollama, LM Studio, vLLM)
    - OpenAI-compatible APIs
    - Custom inference endpoints
    """

    def __init__(
        self,
        api_key: str,
        model: str,
        api_url: str,  # Required for custom endpoints
        **config,
    ):
        super().__init__(api_key, model, api_url, **config)

        if not api_url:
            raise ValueError("Custom LLM client requires api_url")

        self.headers = {
            "Content-Type": "application/json",
        }

        # Support different auth types
        auth_type = config.get("auth_type", "bearer")
        if auth_type == "bearer" and api_key:
            self.headers["Authorization"] = f"Bearer {api_key}"
        elif auth_type == "api_key" and api_key:
            self.headers["X-API-Key"] = api_key

        self.system_message = config.get(
            "system_message",
            "You are a helpful assistant that evaluates AI responses."
        )

        # Endpoint path configuration
        self.completions_path = config.get("completions_path", "/v1/chat/completions")

    @retry(
        stop=stop_after_attempt(settings.retry_attempts),
        wait=wait_exponential(multiplier=settings.retry_delay, min=1, max=10),
    )
    def generate(self, prompt: str, **kwargs) -> str:
        """Generate text using custom API."""
        response = self.generate_with_metadata(prompt, **kwargs)
        return response.content

    @retry(
        stop=stop_after_attempt(settings.retry_attempts),
        wait=wait_exponential(multiplier=settings.retry_delay, min=1, max=10),
    )
    def generate_with_metadata(self, prompt: str, **kwargs) -> LLMResponse:
        """Generate text with full metadata."""
        start_time = time.time()

        url = f"{self.api_url.rstrip('/')}{self.completions_path}"

        payload = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": self.system_message},
                {"role": "user", "content": prompt},
            ],
            "temperature": kwargs.get("temperature", self.temperature),
            "max_tokens": kwargs.get("max_tokens", self.max_tokens),
        }

        # Add any extra parameters from config
        extra_params = self.config.get("extra_params", {})
        payload.update(extra_params)

        try:
            with httpx.Client(timeout=self.timeout) as client:
                response = client.post(url, json=payload, headers=self.headers)
                response.raise_for_status()

            latency_ms = int((time.time() - start_time) * 1000)

            data = response.json()

            # Parse OpenAI-style response
            content = ""
            if "choices" in data and data["choices"]:
                choice = data["choices"][0]
                if "message" in choice:
                    content = choice["message"].get("content", "")
                elif "text" in choice:  # Completions API format
                    content = choice["text"]

            # Parse usage
            usage = data.get("usage", {})
            input_tokens = usage.get("prompt_tokens", 0)
            output_tokens = usage.get("completion_tokens", 0)

            return LLMResponse(
                content=content,
                input_tokens=input_tokens,
                output_tokens=output_tokens,
                total_tokens=input_tokens + output_tokens,
                latency_ms=latency_ms,
                model=f"custom:{self.model}",
                metadata={
                    "endpoint": url,
                    "raw_response": data,
                },
            )

        except httpx.HTTPStatusError as e:
            logger.error(
                "Custom LLM API HTTP error",
                status_code=e.response.status_code,
                response=e.response.text[:500],
            )
            raise
        except Exception as e:
            logger.error("Custom LLM API error", error=str(e), url=url)
            raise
