"""
Exact match and contains metrics.
"""
import re
from typing import Optional

from app.metrics.base import BaseMetric, MetricResult


class ExactMatchMetric(BaseMetric):
    """
    Exact match metric - binary score (0 or 1).

    Compares generated output with ground truth after normalization.
    """

    @property
    def code(self) -> str:
        return "EXACT_MATCH"

    @property
    def requires_ground_truth(self) -> bool:
        return True

    @property
    def requires_context(self) -> bool:
        return False

    @property
    def requires_judge_llm(self) -> bool:
        return False

    def _normalize(self, text: str, case_sensitive: bool = False) -> str:
        """Normalize text for comparison."""
        if not case_sensitive:
            text = text.lower()

        # Remove extra whitespace
        text = re.sub(r'\s+', ' ', text).strip()

        return text

    def calculate(
        self,
        question: str,
        generated_output: str,
        ground_truth: Optional[str] = None,
        context: Optional[list[str]] = None,
        **kwargs,
    ) -> MetricResult:
        """Calculate exact match score."""
        error = self._validate_inputs(generated_output, ground_truth, context)
        if error:
            return MetricResult.from_error(error)

        try:
            case_sensitive = kwargs.get("case_sensitive", False)
            strip_punctuation = kwargs.get("strip_punctuation", False)

            normalized_output = self._normalize(generated_output, case_sensitive)
            normalized_truth = self._normalize(ground_truth, case_sensitive)

            if strip_punctuation:
                normalized_output = re.sub(r'[^\w\s]', '', normalized_output)
                normalized_truth = re.sub(r'[^\w\s]', '', normalized_truth)

            is_match = normalized_output == normalized_truth
            score = 1.0 if is_match else 0.0

            return MetricResult(
                score=score,
                raw_score=score,
                details={
                    "is_exact_match": is_match,
                    "case_sensitive": case_sensitive,
                    "strip_punctuation": strip_punctuation,
                    "output_length": len(normalized_output),
                    "truth_length": len(normalized_truth),
                },
            )

        except Exception as e:
            return MetricResult.from_error(f"Exact match calculation failed: {str(e)}")


class ContainsMetric(BaseMetric):
    """
    Contains metric - checks if generated output contains the ground truth.

    Useful for checking if key information is present in the response.
    """

    @property
    def code(self) -> str:
        return "CONTAINS"

    @property
    def requires_ground_truth(self) -> bool:
        return True

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
        **kwargs,
    ) -> MetricResult:
        """Calculate contains score."""
        error = self._validate_inputs(generated_output, ground_truth, context)
        if error:
            return MetricResult.from_error(error)

        try:
            case_sensitive = kwargs.get("case_sensitive", False)

            if case_sensitive:
                contains = ground_truth in generated_output
            else:
                contains = ground_truth.lower() in generated_output.lower()

            score = 1.0 if contains else 0.0

            return MetricResult(
                score=score,
                raw_score=score,
                details={
                    "contains": contains,
                    "case_sensitive": case_sensitive,
                    "search_term_length": len(ground_truth),
                    "output_length": len(generated_output),
                },
            )

        except Exception as e:
            return MetricResult.from_error(f"Contains calculation failed: {str(e)}")
