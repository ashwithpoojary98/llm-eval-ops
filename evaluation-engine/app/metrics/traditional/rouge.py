"""
ROUGE (Recall-Oriented Understudy for Gisting Evaluation) metric implementations.
"""
from typing import Optional
from rouge_score import rouge_scorer

from app.metrics.base import BaseMetric, MetricResult


class BaseROUGEMetric(BaseMetric):
    """Base class for ROUGE metrics."""

    @property
    def requires_ground_truth(self) -> bool:
        return True

    @property
    def requires_context(self) -> bool:
        return False

    @property
    def requires_judge_llm(self) -> bool:
        return False

    @property
    def rouge_type(self) -> str:
        """ROUGE variant: rouge1, rouge2, or rougeL."""
        raise NotImplementedError

    def calculate(
        self,
        question: str,
        generated_output: str,
        ground_truth: Optional[str] = None,
        context: Optional[list[str]] = None,
        **kwargs,
    ) -> MetricResult:
        """Calculate ROUGE score."""
        error = self._validate_inputs(generated_output, ground_truth, context)
        if error:
            return MetricResult.from_error(error)

        try:
            scorer = rouge_scorer.RougeScorer(
                [self.rouge_type],
                use_stemmer=kwargs.get("use_stemmer", True),
            )

            scores = scorer.score(ground_truth, generated_output)
            rouge_score = scores[self.rouge_type]

            # Use F1 score as the primary metric
            f1_score = rouge_score.fmeasure

            return MetricResult(
                score=f1_score,
                raw_score=f1_score,
                details={
                    "precision": round(rouge_score.precision, 4),
                    "recall": round(rouge_score.recall, 4),
                    "f1": round(f1_score, 4),
                    "rouge_type": self.rouge_type,
                },
            )

        except Exception as e:
            return MetricResult.from_error(f"ROUGE calculation failed: {str(e)}")


class Rouge1Metric(BaseROUGEMetric):
    """ROUGE-1 (unigram) metric."""

    @property
    def code(self) -> str:
        return "ROUGE_1"

    @property
    def rouge_type(self) -> str:
        return "rouge1"


class Rouge2Metric(BaseROUGEMetric):
    """ROUGE-2 (bigram) metric."""

    @property
    def code(self) -> str:
        return "ROUGE_2"

    @property
    def rouge_type(self) -> str:
        return "rouge2"


class RougeLMetric(BaseROUGEMetric):
    """ROUGE-L (longest common subsequence) metric."""

    @property
    def code(self) -> str:
        return "ROUGE_L"

    @property
    def rouge_type(self) -> str:
        return "rougeL"
