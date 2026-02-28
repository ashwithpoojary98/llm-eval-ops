"""
Relevancy metrics for RAG evaluation.
"""
from typing import Optional

from app.metrics.base import BaseMetric, MetricResult


class AnswerRelevancyMetric(BaseMetric):
    """
    Answer Relevancy metric.

    Measures whether the generated answer actually addresses the question.
    Does not require ground truth - evaluates based on question-answer alignment.
    """

    def __init__(self, llm_client=None):
        self.llm_client = llm_client

    @property
    def code(self) -> str:
        return "ANSWER_RELEVANCY"

    @property
    def requires_ground_truth(self) -> bool:
        return False

    @property
    def requires_context(self) -> bool:
        return True

    @property
    def requires_judge_llm(self) -> bool:
        return True

    def calculate(
        self,
        question: str,
        generated_output: str,
        ground_truth: Optional[str] = None,
        context: Optional[list[str]] = None,
        **kwargs,
    ) -> MetricResult:
        """Calculate answer relevancy score."""
        error = self._validate_inputs(generated_output, ground_truth, context)
        if error:
            return MetricResult.from_error(error)

        if not self.llm_client:
            return MetricResult.from_error("LLM client required for Answer Relevancy metric")

        try:
            prompt = f"""You are evaluating whether an answer is relevant to the question asked.

Question: {question}

Answer: {generated_output}

Evaluate whether the answer:
1. Directly addresses what was asked
2. Provides the type of information requested
3. Stays on topic without excessive tangents
4. Is complete enough to be useful

Provide your evaluation in the following format:
SCORE: [0.0 to 1.0, where 1.0 means perfectly relevant]
REASONING: [Brief explanation of your score]"""

            response = self.llm_client.generate(prompt)
            score, reasoning = self._parse_judge_response(response)

            return MetricResult(
                score=score,
                raw_score=score,
                reasoning=reasoning,
                details={
                    "question_length": len(question),
                    "answer_length": len(generated_output),
                },
            )

        except Exception as e:
            return MetricResult.from_error(f"Answer relevancy calculation failed: {str(e)}")

    def _parse_judge_response(self, response: str) -> tuple[float, str]:
        """Parse LLM judge response."""
        lines = response.strip().split("\n")
        score = 0.5
        reasoning = response

        for line in lines:
            line = line.strip()
            if line.upper().startswith("SCORE:"):
                try:
                    score_text = line.split(":", 1)[1].strip()
                    if "/" in score_text:
                        parts = score_text.split("/")
                        score = float(parts[0]) / float(parts[1])
                    else:
                        score = float(score_text)
                    score = max(0.0, min(1.0, score))
                except (ValueError, IndexError):
                    pass
            elif line.upper().startswith("REASONING:"):
                reasoning = line.split(":", 1)[1].strip()

        return score, reasoning


class ContextRelevancyMetric(BaseMetric):
    """
    Context Relevancy metric.

    Measures how relevant the retrieved context is to the question.
    Helps identify retrieval quality issues.
    """

    def __init__(self, llm_client=None):
        self.llm_client = llm_client

    @property
    def code(self) -> str:
        return "CONTEXT_RELEVANCY"

    @property
    def requires_ground_truth(self) -> bool:
        return False

    @property
    def requires_context(self) -> bool:
        return True

    @property
    def requires_judge_llm(self) -> bool:
        return True

    def calculate(
        self,
        question: str,
        generated_output: str,
        ground_truth: Optional[str] = None,
        context: Optional[list[str]] = None,
        **kwargs,
    ) -> MetricResult:
        """Calculate context relevancy score."""
        error = self._validate_inputs(generated_output, ground_truth, context)
        if error:
            return MetricResult.from_error(error)

        if not self.llm_client:
            return MetricResult.from_error("LLM client required for Context Relevancy metric")

        try:
            context_text = "\n\n".join(f"Context {i+1}: {c}" for i, c in enumerate(context))

            prompt = f"""You are evaluating the relevancy of retrieved context chunks to a question.

Question: {question}

Retrieved Context:
{context_text}

For each context chunk, consider:
1. Does it contain information relevant to answering the question?
2. Would this context help an AI generate a good answer?
3. Is there irrelevant or noise content?

Provide an overall relevancy score and reasoning:
SCORE: [0.0 to 1.0, where 1.0 means all context is highly relevant]
REASONING: [Brief explanation identifying which contexts are useful vs. irrelevant]"""

            response = self.llm_client.generate(prompt)
            score, reasoning = self._parse_judge_response(response)

            return MetricResult(
                score=score,
                raw_score=score,
                reasoning=reasoning,
                details={
                    "context_chunks": len(context),
                    "total_context_length": sum(len(c) for c in context),
                },
            )

        except Exception as e:
            return MetricResult.from_error(f"Context relevancy calculation failed: {str(e)}")

    def _parse_judge_response(self, response: str) -> tuple[float, str]:
        """Parse LLM judge response."""
        lines = response.strip().split("\n")
        score = 0.5
        reasoning = response

        for line in lines:
            line = line.strip()
            if line.upper().startswith("SCORE:"):
                try:
                    score_text = line.split(":", 1)[1].strip()
                    if "/" in score_text:
                        parts = score_text.split("/")
                        score = float(parts[0]) / float(parts[1])
                    else:
                        score = float(score_text)
                    score = max(0.0, min(1.0, score))
                except (ValueError, IndexError):
                    pass
            elif line.upper().startswith("REASONING:"):
                reasoning = line.split(":", 1)[1].strip()

        return score, reasoning
