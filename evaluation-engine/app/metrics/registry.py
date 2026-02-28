"""
Metric registry - factory for creating metric instances.
"""
from typing import Optional
from functools import lru_cache

from app.models.enums import MetricCode
from app.metrics.base import BaseMetric

# Import all metric implementations
from app.metrics.traditional import (
    BLEUMetric,
    Rouge1Metric,
    Rouge2Metric,
    RougeLMetric,
    ExactMatchMetric,
    ContainsMetric,
    LevenshteinMetric,
)
from app.metrics.semantic import (
    SemanticSimilarityMetric,
    BERTScoreMetric,
)
from app.metrics.rag import (
    FaithfulnessMetric,
    AnswerRelevancyMetric,
    ContextRelevancyMetric,
    ContextPrecisionMetric,
    ContextRecallMetric,
)
from app.metrics.llm_judge import (
    RelevanceJudgeMetric,
    CoherenceJudgeMetric,
    FluencyJudgeMetric,
    ToxicityJudgeMetric,
    AnswerCorrectnessMetric,
    CustomJudgeMetric,
)
from app.metrics.performance import (
    LatencyMetric,
    TokenCountMetric,
    CostMetric,
)


class MetricRegistry:
    """
    Registry for creating metric instances.

    Handles metric instantiation with optional LLM client injection
    for metrics that require judge capabilities.
    """

    # Mapping of metric codes to their implementation classes
    _metric_classes: dict[MetricCode, type] = {
        # Traditional NLP
        MetricCode.BLEU: BLEUMetric,
        MetricCode.ROUGE_1: Rouge1Metric,
        MetricCode.ROUGE_2: Rouge2Metric,
        MetricCode.ROUGE_L: RougeLMetric,
        MetricCode.EXACT_MATCH: ExactMatchMetric,
        MetricCode.CONTAINS: ContainsMetric,
        MetricCode.LEVENSHTEIN: LevenshteinMetric,

        # Semantic
        MetricCode.SEMANTIC_SIMILARITY: SemanticSimilarityMetric,
        MetricCode.BERTSCORE: BERTScoreMetric,

        # RAG-specific
        MetricCode.FAITHFULNESS: FaithfulnessMetric,
        MetricCode.ANSWER_RELEVANCY: AnswerRelevancyMetric,
        MetricCode.CONTEXT_RELEVANCY: ContextRelevancyMetric,
        MetricCode.CONTEXT_PRECISION: ContextPrecisionMetric,
        MetricCode.CONTEXT_RECALL: ContextRecallMetric,

        # LLM-as-Judge
        MetricCode.RELEVANCE: RelevanceJudgeMetric,
        MetricCode.COHERENCE: CoherenceJudgeMetric,
        MetricCode.FLUENCY: FluencyJudgeMetric,
        MetricCode.TOXICITY: ToxicityJudgeMetric,
        MetricCode.ANSWER_CORRECTNESS: AnswerCorrectnessMetric,
        MetricCode.CUSTOM_JUDGE: CustomJudgeMetric,

        # Performance
        MetricCode.LATENCY: LatencyMetric,
        MetricCode.TOKEN_COUNT: TokenCountMetric,
        MetricCode.COST: CostMetric,
    }

    # Metrics that require LLM client
    _llm_required_metrics = {
        MetricCode.FAITHFULNESS,
        MetricCode.ANSWER_RELEVANCY,
        MetricCode.CONTEXT_RELEVANCY,
        MetricCode.CONTEXT_PRECISION,
        MetricCode.CONTEXT_RECALL,
        MetricCode.RELEVANCE,
        MetricCode.COHERENCE,
        MetricCode.FLUENCY,
        MetricCode.TOXICITY,
        MetricCode.ANSWER_CORRECTNESS,
        MetricCode.CUSTOM_JUDGE,
    }

    def get(
        self,
        metric_code: MetricCode,
        llm_client=None,
        prompt_template: Optional[str] = None,
    ) -> BaseMetric:
        """
        Get a metric instance by code.

        Args:
            metric_code: The metric code to instantiate
            llm_client: LLM client for judge-based metrics
            prompt_template: Custom prompt template for judge metrics

        Returns:
            Configured metric instance
        """
        metric_class = self._metric_classes.get(metric_code)

        if not metric_class:
            raise ValueError(f"Unknown metric code: {metric_code}")

        # Check if metric requires LLM client
        if metric_code in self._llm_required_metrics:
            if prompt_template:
                return metric_class(llm_client=llm_client, prompt_template=prompt_template)
            return metric_class(llm_client=llm_client)

        # Simple metrics don't need any arguments
        return metric_class()

    def is_available(self, metric_code: MetricCode) -> bool:
        """Check if a metric is available."""
        return metric_code in self._metric_classes

    def requires_llm(self, metric_code: MetricCode) -> bool:
        """Check if a metric requires an LLM client."""
        return metric_code in self._llm_required_metrics

    def list_metrics(self) -> list[MetricCode]:
        """List all available metrics."""
        return list(self._metric_classes.keys())


@lru_cache
def get_metric_registry() -> MetricRegistry:
    """Get cached metric registry instance."""
    return MetricRegistry()
