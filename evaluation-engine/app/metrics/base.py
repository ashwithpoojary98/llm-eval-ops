"""
Base class for all evaluation metrics.
"""
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Optional, Any


@dataclass
class MetricResult:
    """Result of a metric calculation."""

    score: float  # Normalized score (0-1)
    raw_score: float  # Original/raw score value
    passed: Optional[bool] = None  # Pass/fail if threshold provided
    details: dict[str, Any] = field(default_factory=dict)  # Metric-specific breakdown
    reasoning: Optional[str] = None  # LLM judge reasoning
    error: Optional[str] = None  # Error message if calculation failed

    @classmethod
    def from_error(cls, error: str) -> "MetricResult":
        """Create a result indicating an error."""
        return cls(
            score=0.0,
            raw_score=0.0,
            passed=False,
            error=error,
        )


class BaseMetric(ABC):
    """Abstract base class for all evaluation metrics."""

    @property
    @abstractmethod
    def code(self) -> str:
        """Unique metric code identifier."""
        pass

    @property
    @abstractmethod
    def requires_ground_truth(self) -> bool:
        """Whether this metric requires ground truth reference."""
        pass

    @property
    @abstractmethod
    def requires_context(self) -> bool:
        """Whether this metric requires retrieved context (for RAG)."""
        pass

    @property
    @abstractmethod
    def requires_judge_llm(self) -> bool:
        """Whether this metric requires an LLM judge."""
        pass

    @abstractmethod
    def calculate(
        self,
        question: str,
        generated_output: str,
        ground_truth: Optional[str] = None,
        context: Optional[list[str]] = None,
        **kwargs,
    ) -> MetricResult:
        """
        Calculate the metric score.

        Args:
            question: The input question/prompt
            generated_output: The LLM-generated response
            ground_truth: Expected correct answer (if applicable)
            context: Retrieved context chunks (for RAG evaluation)
            **kwargs: Additional metric-specific parameters

        Returns:
            MetricResult with score and details
        """
        pass

    def _validate_inputs(
        self,
        generated_output: str,
        ground_truth: Optional[str],
        context: Optional[list[str]],
    ) -> Optional[str]:
        """Validate inputs before calculation. Returns error message if invalid."""
        if not generated_output:
            return "Generated output is required"

        if self.requires_ground_truth and not ground_truth:
            return f"{self.code} requires ground truth reference"

        if self.requires_context and not context:
            return f"{self.code} requires retrieved context"

        return None

    def with_threshold(
        self,
        result: MetricResult,
        threshold: Optional[float],
    ) -> MetricResult:
        """Apply pass/fail threshold to result."""
        if threshold is not None:
            result.passed = result.score >= threshold
        return result
