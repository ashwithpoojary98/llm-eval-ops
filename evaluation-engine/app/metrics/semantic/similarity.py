"""
Semantic similarity metric using sentence embeddings.
"""
from typing import Optional
import numpy as np
from functools import lru_cache

from app.metrics.base import BaseMetric, MetricResult
from app.config import get_settings

settings = get_settings()

# Lazy load sentence transformer model
_model = None


def get_embedding_model():
    """Lazy load the embedding model."""
    global _model
    if _model is None:
        from sentence_transformers import SentenceTransformer
        _model = SentenceTransformer(settings.embedding_model)
    return _model


class SemanticSimilarityMetric(BaseMetric):
    """
    Semantic similarity using sentence embeddings.

    Computes cosine similarity between embeddings of generated output
    and ground truth using a pre-trained sentence transformer model.
    """

    @property
    def code(self) -> str:
        return "SEMANTIC_SIMILARITY"

    @property
    def requires_ground_truth(self) -> bool:
        return True

    @property
    def requires_context(self) -> bool:
        return False

    @property
    def requires_judge_llm(self) -> bool:
        return False

    def _cosine_similarity(self, vec1: np.ndarray, vec2: np.ndarray) -> float:
        """Calculate cosine similarity between two vectors."""
        dot_product = np.dot(vec1, vec2)
        norm1 = np.linalg.norm(vec1)
        norm2 = np.linalg.norm(vec2)

        if norm1 == 0 or norm2 == 0:
            return 0.0

        return float(dot_product / (norm1 * norm2))

    def calculate(
        self,
        question: str,
        generated_output: str,
        ground_truth: Optional[str] = None,
        context: Optional[list[str]] = None,
        **kwargs,
    ) -> MetricResult:
        """Calculate semantic similarity score."""
        error = self._validate_inputs(generated_output, ground_truth, context)
        if error:
            return MetricResult.from_error(error)

        try:
            model = get_embedding_model()

            # Get embeddings
            embeddings = model.encode(
                [generated_output, ground_truth],
                convert_to_numpy=True,
            )

            output_embedding = embeddings[0]
            truth_embedding = embeddings[1]

            # Calculate cosine similarity
            similarity = self._cosine_similarity(output_embedding, truth_embedding)

            # Normalize to 0-1 range (cosine similarity is already -1 to 1)
            # For text, we typically expect positive similarity
            normalized_score = max(0.0, similarity)

            return MetricResult(
                score=normalized_score,
                raw_score=similarity,
                details={
                    "cosine_similarity": round(similarity, 4),
                    "model": settings.embedding_model,
                    "embedding_dim": len(output_embedding),
                },
            )

        except Exception as e:
            return MetricResult.from_error(f"Semantic similarity calculation failed: {str(e)}")
