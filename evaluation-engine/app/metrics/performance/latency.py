"""
Latency metric for measuring LLM response time.
"""
from typing import Optional

from app.metrics.base import BaseMetric, MetricResult


class LatencyMetric(BaseMetric):
    """
    Measures LLM response latency.

    Score is normalized based on a target latency threshold.
    A response under the target gets score 1.0, degrading as latency increases.
    """

    # Default target latency in milliseconds
    DEFAULT_TARGET_MS = 2000

    @property
    def code(self) -> str:
        return "LATENCY"

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
        latency_ms: Optional[int] = None,
        target_latency_ms: Optional[int] = None,
        **kwargs,
    ) -> MetricResult:
        """
        Calculate latency score.

        Args:
            latency_ms: Actual latency in milliseconds
            target_latency_ms: Target latency threshold
        """
        if latency_ms is None:
            return MetricResult.from_error("Latency data not provided")

        target = target_latency_ms or self.DEFAULT_TARGET_MS

        # Score is 1.0 if under target, decreases as latency increases
        if latency_ms <= target:
            score = 1.0
        else:
            # Exponential decay for scores above target
            ratio = latency_ms / target
            score = max(0.0, 1.0 - (ratio - 1) / 5)  # 0 at 6x target

        return MetricResult(
            score=score,
            raw_score=float(latency_ms),
            details={
                "latency_ms": latency_ms,
                "target_ms": target,
                "within_target": latency_ms <= target,
            },
        )
