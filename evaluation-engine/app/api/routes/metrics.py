"""
Metrics information endpoints.
"""
from fastapi import APIRouter

from app.models.enums import MetricCode, MetricCategory
from app.models.responses import MetricInfo

router = APIRouter(prefix="/metrics", tags=["metrics"])

# Metric definitions with metadata
METRIC_DEFINITIONS: dict[MetricCode, MetricInfo] = {
    # Traditional NLP Metrics
    MetricCode.BLEU: MetricInfo(
        code=MetricCode.BLEU,
        name="BLEU Score",
        description="Bilingual Evaluation Understudy - measures n-gram overlap between generated and reference text",
        category=MetricCategory.TRADITIONAL_NLP,
        requires_ground_truth=True,
        requires_context=False,
        requires_judge_llm=False,
        default_threshold=0.3,
    ),
    MetricCode.ROUGE_1: MetricInfo(
        code=MetricCode.ROUGE_1,
        name="ROUGE-1",
        description="Recall-Oriented Understudy for Gisting Evaluation - unigram overlap",
        category=MetricCategory.TRADITIONAL_NLP,
        requires_ground_truth=True,
        requires_context=False,
        requires_judge_llm=False,
        default_threshold=0.4,
    ),
    MetricCode.ROUGE_2: MetricInfo(
        code=MetricCode.ROUGE_2,
        name="ROUGE-2",
        description="ROUGE with bigram overlap",
        category=MetricCategory.TRADITIONAL_NLP,
        requires_ground_truth=True,
        requires_context=False,
        requires_judge_llm=False,
        default_threshold=0.2,
    ),
    MetricCode.ROUGE_L: MetricInfo(
        code=MetricCode.ROUGE_L,
        name="ROUGE-L",
        description="ROUGE with longest common subsequence",
        category=MetricCategory.TRADITIONAL_NLP,
        requires_ground_truth=True,
        requires_context=False,
        requires_judge_llm=False,
        default_threshold=0.35,
    ),
    MetricCode.EXACT_MATCH: MetricInfo(
        code=MetricCode.EXACT_MATCH,
        name="Exact Match",
        description="Binary score - 1 if generated text exactly matches reference (case-insensitive)",
        category=MetricCategory.TRADITIONAL_NLP,
        requires_ground_truth=True,
        requires_context=False,
        requires_judge_llm=False,
        default_threshold=1.0,
    ),
    MetricCode.CONTAINS: MetricInfo(
        code=MetricCode.CONTAINS,
        name="Contains Match",
        description="Binary score - 1 if generated text contains the reference text",
        category=MetricCategory.TRADITIONAL_NLP,
        requires_ground_truth=True,
        requires_context=False,
        requires_judge_llm=False,
        default_threshold=1.0,
    ),
    MetricCode.LEVENSHTEIN: MetricInfo(
        code=MetricCode.LEVENSHTEIN,
        name="Levenshtein Similarity",
        description="Edit distance-based similarity (1 - normalized_distance)",
        category=MetricCategory.TRADITIONAL_NLP,
        requires_ground_truth=True,
        requires_context=False,
        requires_judge_llm=False,
        default_threshold=0.7,
    ),

    # Semantic Metrics
    MetricCode.SEMANTIC_SIMILARITY: MetricInfo(
        code=MetricCode.SEMANTIC_SIMILARITY,
        name="Semantic Similarity",
        description="Cosine similarity between sentence embeddings",
        category=MetricCategory.SEMANTIC,
        requires_ground_truth=True,
        requires_context=False,
        requires_judge_llm=False,
        default_threshold=0.7,
    ),
    MetricCode.BERTSCORE: MetricInfo(
        code=MetricCode.BERTSCORE,
        name="BERTScore",
        description="Contextual embedding similarity using BERT",
        category=MetricCategory.SEMANTIC,
        requires_ground_truth=True,
        requires_context=False,
        requires_judge_llm=False,
        default_threshold=0.7,
    ),

    # RAG-Specific Metrics
    MetricCode.FAITHFULNESS: MetricInfo(
        code=MetricCode.FAITHFULNESS,
        name="Faithfulness",
        description="Measures if the answer is grounded in the provided context (no hallucination)",
        category=MetricCategory.RAG_SPECIFIC,
        requires_ground_truth=False,
        requires_context=True,
        requires_judge_llm=True,
        default_threshold=0.7,
    ),
    MetricCode.ANSWER_RELEVANCY: MetricInfo(
        code=MetricCode.ANSWER_RELEVANCY,
        name="Answer Relevancy",
        description="Measures if the answer addresses the question",
        category=MetricCategory.RAG_SPECIFIC,
        requires_ground_truth=False,
        requires_context=True,
        requires_judge_llm=True,
        default_threshold=0.7,
    ),
    MetricCode.CONTEXT_PRECISION: MetricInfo(
        code=MetricCode.CONTEXT_PRECISION,
        name="Context Precision",
        description="Measures if retrieved context is useful for answering",
        category=MetricCategory.RAG_SPECIFIC,
        requires_ground_truth=True,
        requires_context=True,
        requires_judge_llm=True,
        default_threshold=0.7,
    ),
    MetricCode.CONTEXT_RECALL: MetricInfo(
        code=MetricCode.CONTEXT_RECALL,
        name="Context Recall",
        description="Measures if all relevant information was retrieved",
        category=MetricCategory.RAG_SPECIFIC,
        requires_ground_truth=True,
        requires_context=True,
        requires_judge_llm=True,
        default_threshold=0.7,
    ),
    MetricCode.CONTEXT_RELEVANCY: MetricInfo(
        code=MetricCode.CONTEXT_RELEVANCY,
        name="Context Relevancy",
        description="Measures relevance of retrieved context to the question",
        category=MetricCategory.RAG_SPECIFIC,
        requires_ground_truth=False,
        requires_context=True,
        requires_judge_llm=True,
        default_threshold=0.7,
    ),

    # LLM-as-Judge Metrics
    MetricCode.RELEVANCE: MetricInfo(
        code=MetricCode.RELEVANCE,
        name="Relevance (Judge)",
        description="LLM judges how relevant the response is to the query",
        category=MetricCategory.LLM_AS_JUDGE,
        requires_ground_truth=False,
        requires_context=False,
        requires_judge_llm=True,
        default_threshold=0.6,
    ),
    MetricCode.COHERENCE: MetricInfo(
        code=MetricCode.COHERENCE,
        name="Coherence (Judge)",
        description="LLM judges logical flow and structure of response",
        category=MetricCategory.LLM_AS_JUDGE,
        requires_ground_truth=False,
        requires_context=False,
        requires_judge_llm=True,
        default_threshold=0.6,
    ),
    MetricCode.FLUENCY: MetricInfo(
        code=MetricCode.FLUENCY,
        name="Fluency (Judge)",
        description="LLM judges grammar and readability",
        category=MetricCategory.LLM_AS_JUDGE,
        requires_ground_truth=False,
        requires_context=False,
        requires_judge_llm=True,
        default_threshold=0.6,
    ),
    MetricCode.TOXICITY: MetricInfo(
        code=MetricCode.TOXICITY,
        name="Toxicity Detection",
        description="LLM detects harmful or inappropriate content (lower is better)",
        category=MetricCategory.LLM_AS_JUDGE,
        requires_ground_truth=False,
        requires_context=False,
        requires_judge_llm=True,
        default_threshold=0.2,
    ),
    MetricCode.ANSWER_CORRECTNESS: MetricInfo(
        code=MetricCode.ANSWER_CORRECTNESS,
        name="Answer Correctness (Judge)",
        description="LLM judges if the answer is factually correct",
        category=MetricCategory.LLM_AS_JUDGE,
        requires_ground_truth=True,
        requires_context=False,
        requires_judge_llm=True,
        default_threshold=0.7,
    ),
    MetricCode.CUSTOM_JUDGE: MetricInfo(
        code=MetricCode.CUSTOM_JUDGE,
        name="Custom Judge Metric",
        description="Custom evaluation using user-defined prompt template",
        category=MetricCategory.LLM_AS_JUDGE,
        requires_ground_truth=False,
        requires_context=False,
        requires_judge_llm=True,
        default_threshold=None,
    ),

    # Performance Metrics
    MetricCode.LATENCY: MetricInfo(
        code=MetricCode.LATENCY,
        name="Response Latency",
        description="Time taken for LLM to respond (in milliseconds)",
        category=MetricCategory.PERFORMANCE,
        requires_ground_truth=False,
        requires_context=False,
        requires_judge_llm=False,
        default_threshold=None,
    ),
    MetricCode.TOKEN_COUNT: MetricInfo(
        code=MetricCode.TOKEN_COUNT,
        name="Token Count",
        description="Total tokens used (input + output)",
        category=MetricCategory.PERFORMANCE,
        requires_ground_truth=False,
        requires_context=False,
        requires_judge_llm=False,
        default_threshold=None,
    ),
    MetricCode.COST: MetricInfo(
        code=MetricCode.COST,
        name="API Cost",
        description="Estimated API cost in USD",
        category=MetricCategory.PERFORMANCE,
        requires_ground_truth=False,
        requires_context=False,
        requires_judge_llm=False,
        default_threshold=None,
    ),
}


@router.get("", response_model=list[MetricInfo])
async def list_metrics():
    """List all supported metrics with their requirements."""
    return list(METRIC_DEFINITIONS.values())


@router.get("/categories")
async def list_categories():
    """List metric categories."""
    return [
        {
            "code": cat.value,
            "name": cat.value.replace("_", " ").title(),
            "metrics": [
                m.code for m in METRIC_DEFINITIONS.values()
                if m.category == cat
            ]
        }
        for cat in MetricCategory
    ]


@router.get("/{metric_code}", response_model=MetricInfo)
async def get_metric_info(metric_code: MetricCode):
    """Get detailed information about a specific metric."""
    return METRIC_DEFINITIONS[metric_code]
