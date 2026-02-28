"""
BLEU (Bilingual Evaluation Understudy) metric implementation.
"""
from typing import Optional
from nltk.translate.bleu_score import sentence_bleu, SmoothingFunction
from nltk.tokenize import word_tokenize

from app.metrics.base import BaseMetric, MetricResult


class BLEUMetric(BaseMetric):
    """
    BLEU score metric for evaluating text generation quality.

    Measures n-gram overlap between generated text and reference.
    Uses smoothing to handle cases with zero n-gram matches.
    """

    @property
    def code(self) -> str:
        return "BLEU"

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
        """Calculate BLEU score."""
        # Validate inputs
        error = self._validate_inputs(generated_output, ground_truth, context)
        if error:
            return MetricResult.from_error(error)

        try:
            # Tokenize
            reference_tokens = word_tokenize(ground_truth.lower())
            hypothesis_tokens = word_tokenize(generated_output.lower())

            # Handle empty cases
            if not reference_tokens or not hypothesis_tokens:
                return MetricResult(
                    score=0.0,
                    raw_score=0.0,
                    details={
                        "reference_length": len(reference_tokens),
                        "hypothesis_length": len(hypothesis_tokens),
                        "note": "Empty tokenization",
                    },
                )

            # Use smoothing to handle zero matches
            smoothie = SmoothingFunction().method1

            # Calculate BLEU with different n-gram weights
            # Default: (0.25, 0.25, 0.25, 0.25) for BLEU-4
            weights = kwargs.get("weights", (0.25, 0.25, 0.25, 0.25))

            score = sentence_bleu(
                [reference_tokens],
                hypothesis_tokens,
                weights=weights,
                smoothing_function=smoothie,
            )

            # Also calculate individual n-gram scores
            bleu_1 = sentence_bleu(
                [reference_tokens], hypothesis_tokens,
                weights=(1, 0, 0, 0), smoothing_function=smoothie
            )
            bleu_2 = sentence_bleu(
                [reference_tokens], hypothesis_tokens,
                weights=(0.5, 0.5, 0, 0), smoothing_function=smoothie
            )
            bleu_3 = sentence_bleu(
                [reference_tokens], hypothesis_tokens,
                weights=(0.33, 0.33, 0.33, 0), smoothing_function=smoothie
            )
            bleu_4 = sentence_bleu(
                [reference_tokens], hypothesis_tokens,
                weights=(0.25, 0.25, 0.25, 0.25), smoothing_function=smoothie
            )

            return MetricResult(
                score=score,
                raw_score=score,
                details={
                    "bleu_1": round(bleu_1, 4),
                    "bleu_2": round(bleu_2, 4),
                    "bleu_3": round(bleu_3, 4),
                    "bleu_4": round(bleu_4, 4),
                    "reference_length": len(reference_tokens),
                    "hypothesis_length": len(hypothesis_tokens),
                    "weights": weights,
                },
            )

        except Exception as e:
            return MetricResult.from_error(f"BLEU calculation failed: {str(e)}")
