"""
BERTScore metric implementation.
"""
from typing import Optional

from app.metrics.base import BaseMetric, MetricResult
from app.config import get_settings

settings = get_settings()


class BERTScoreMetric(BaseMetric):
    """
    BERTScore metric for evaluating text generation quality.

    Uses contextual embeddings from BERT to compute similarity
    between generated and reference text at the token level.
    """

    @property
    def code(self) -> str:
        return "BERTSCORE"

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
        """Calculate BERTScore."""
        error = self._validate_inputs(generated_output, ground_truth, context)
        if error:
            return MetricResult.from_error(error)

        try:
            from bert_score import score as bert_score

            # Calculate BERTScore
            # Returns (precision, recall, f1) tensors
            P, R, F1 = bert_score(
                cands=[generated_output],
                refs=[ground_truth],
                lang="en",
                model_type=kwargs.get("model_type", settings.bertscore_model),
                rescale_with_baseline=kwargs.get("rescale_with_baseline", True),
                verbose=False,
            )

            precision = float(P[0])
            recall = float(R[0])
            f1 = float(F1[0])

            # Use F1 as the primary score
            return MetricResult(
                score=max(0.0, f1),  # Ensure non-negative after baseline rescaling
                raw_score=f1,
                details={
                    "precision": round(precision, 4),
                    "recall": round(recall, 4),
                    "f1": round(f1, 4),
                    "model": kwargs.get("model_type", settings.bertscore_model),
                    "rescaled": kwargs.get("rescale_with_baseline", True),
                },
            )

        except Exception as e:
            return MetricResult.from_error(f"BERTScore calculation failed: {str(e)}")
