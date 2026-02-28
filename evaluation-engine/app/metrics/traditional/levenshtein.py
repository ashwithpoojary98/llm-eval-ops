"""
Levenshtein distance-based similarity metric.
"""
from typing import Optional
import Levenshtein

from app.metrics.base import BaseMetric, MetricResult


class LevenshteinMetric(BaseMetric):
    """
    Levenshtein similarity metric.

    Calculates edit distance between generated output and ground truth,
    then normalizes to a similarity score (1 - normalized_distance).
    """

    @property
    def code(self) -> str:
        return "LEVENSHTEIN"

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
        """Calculate Levenshtein similarity score."""
        error = self._validate_inputs(generated_output, ground_truth, context)
        if error:
            return MetricResult.from_error(error)

        try:
            case_sensitive = kwargs.get("case_sensitive", False)

            if not case_sensitive:
                generated_output = generated_output.lower()
                ground_truth = ground_truth.lower()

            # Calculate edit distance
            distance = Levenshtein.distance(generated_output, ground_truth)

            # Normalize by max length
            max_len = max(len(generated_output), len(ground_truth))
            if max_len == 0:
                similarity = 1.0
            else:
                similarity = 1 - (distance / max_len)

            # Also calculate ratio (sequence matcher style)
            ratio = Levenshtein.ratio(generated_output, ground_truth)

            return MetricResult(
                score=similarity,
                raw_score=float(distance),
                details={
                    "edit_distance": distance,
                    "similarity": round(similarity, 4),
                    "ratio": round(ratio, 4),
                    "output_length": len(generated_output),
                    "truth_length": len(ground_truth),
                    "case_sensitive": case_sensitive,
                },
            )

        except Exception as e:
            return MetricResult.from_error(f"Levenshtein calculation failed: {str(e)}")
