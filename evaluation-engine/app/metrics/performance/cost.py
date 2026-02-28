"""
Cost metric for measuring LLM API costs.
"""
from typing import Optional

from app.metrics.base import BaseMetric, MetricResult


class CostMetric(BaseMetric):
    """
    Tracks API cost for LLM calls.

    Score is normalized based on a target cost threshold.
    Lower cost = higher score.
    """

    # Default target cost in USD per call
    DEFAULT_TARGET_COST = 0.01

    # Default pricing (per 1K tokens)
    DEFAULT_INPUT_COST_PER_1K = 0.001
    DEFAULT_OUTPUT_COST_PER_1K = 0.002

    @property
    def code(self) -> str:
        return "COST"

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
        cost_usd: Optional[float] = None,
        target_cost: Optional[float] = None,
        input_cost_per_1k: Optional[float] = None,
        output_cost_per_1k: Optional[float] = None,
        **kwargs,
    ) -> MetricResult:
        """
        Calculate cost score.

        Args:
            input_tokens: Number of input tokens used
            output_tokens: Number of output tokens generated
            cost_usd: Pre-calculated cost in USD (if available)
            target_cost: Target cost threshold in USD
            input_cost_per_1k: Cost per 1K input tokens
            output_cost_per_1k: Cost per 1K output tokens
        """
        input_tokens = input_tokens or 0
        output_tokens = output_tokens or 0

        # Calculate cost if not provided
        if cost_usd is None:
            input_rate = input_cost_per_1k or self.DEFAULT_INPUT_COST_PER_1K
            output_rate = output_cost_per_1k or self.DEFAULT_OUTPUT_COST_PER_1K
            cost_usd = (input_tokens * input_rate / 1000) + (output_tokens * output_rate / 1000)

        target = target_cost or self.DEFAULT_TARGET_COST

        # Score is 1.0 if under target, decreases as cost increases
        if cost_usd <= target:
            score = 1.0
        else:
            ratio = cost_usd / target
            score = max(0.0, 1.0 - (ratio - 1) / 5)  # 0 at 6x target

        return MetricResult(
            score=score,
            raw_score=cost_usd,
            details={
                "cost_usd": cost_usd,
                "target_cost": target,
                "input_tokens": input_tokens,
                "output_tokens": output_tokens,
                "within_target": cost_usd <= target,
            },
        )
