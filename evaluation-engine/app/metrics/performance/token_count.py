"""
Token count metric for measuring LLM token usage.
"""
from typing import Optional

from app.metrics.base import BaseMetric, MetricResult


class TokenCountMetric(BaseMetric):
    """
    Tracks token usage for LLM calls.

    Score is normalized based on a target token limit.
    Lower token usage = higher score.
    """

    # Default target token limit
    DEFAULT_TARGET_TOKENS = 1000

    @property
    def code(self) -> str:
        return "TOKEN_COUNT"

    @property
    def requires_ground_truth(self) -> bool:
        return False

    @property
    def requires_context(self) -> bool:
        return False

    @property
    def requires_judge_llm(self) -> bool:
        return False

    def calculate(
        self,
        question: str,
        generated_output: str,
        ground_truth: Optional[str] = None,
        context: Optional[list[str]] = None,
        input_tokens: Optional[int] = None,
        output_tokens: Optional[int] = None,
        target_tokens: Optional[int] = None,
        **kwargs,
    ) -> MetricResult:
        """
        Calculate token usage score.

        Args:
            input_tokens: Number of input tokens used
            output_tokens: Number of output tokens generated
            target_tokens: Target total token limit
        """
        input_tokens = input_tokens or 0
        output_tokens = output_tokens or 0
        total_tokens = input_tokens + output_tokens

        target = target_tokens or self.DEFAULT_TARGET_TOKENS

        # Score is 1.0 if under target, decreases as tokens increase
        if total_tokens <= target:
            score = 1.0
        else:
            ratio = total_tokens / target
            score = max(0.0, 1.0 - (ratio - 1) / 3)  # 0 at 4x target

        return MetricResult(
            score=score,
            raw_score=float(total_tokens),
            details={
                "input_tokens": input_tokens,
                "output_tokens": output_tokens,
                "total_tokens": total_tokens,
                "target_tokens": target,
                "within_target": total_tokens <= target,
            },
        )
