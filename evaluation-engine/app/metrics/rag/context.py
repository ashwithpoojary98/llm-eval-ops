"""
Context precision and recall metrics for RAG evaluation.
"""
from typing import Optional

from app.metrics.base import BaseMetric, MetricResult


class ContextPrecisionMetric(BaseMetric):
    """
    Context Precision metric.

    Measures whether the retrieved context chunks that are ranked higher
    are more relevant to answering the question. Requires ground truth
    to determine what information was needed.
    """

    def __init__(self, llm_client=None):
        self.llm_client = llm_client

    @property
    def code(self) -> str:
        return "CONTEXT_PRECISION"

    @property
    def requires_ground_truth(self) -> bool:
        return True

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
        """Calculate context precision score."""
        error = self._validate_inputs(generated_output, ground_truth, context)
        if error:
            return MetricResult.from_error(error)

        if not self.llm_client:
            return MetricResult.from_error("LLM client required for Context Precision metric")

        try:
            context_text = "\n\n".join(f"Context {i+1}: {c}" for i, c in enumerate(context))

            prompt = f"""You are evaluating context precision for a RAG system.

Question: {question}
Ground Truth Answer: {ground_truth}

Retrieved Context (in order of ranking):
{context_text}

For each context chunk, determine if it contains information needed to produce the ground truth answer.
Mark each as RELEVANT or NOT_RELEVANT.

Then calculate precision: What proportion of the retrieved contexts are actually relevant?
Consider ranking - highly ranked irrelevant contexts are worse than low-ranked ones.

Provide your evaluation:
RELEVANT_CHUNKS: [comma-separated list of chunk numbers that are relevant, e.g., "1, 3"]
SCORE: [0.0 to 1.0, precision score considering ranking]
REASONING: [Brief explanation]"""

            response = self.llm_client.generate(prompt)
            score, reasoning, relevant_chunks = self._parse_response(response, len(context))

            return MetricResult(
                score=score,
                raw_score=score,
                reasoning=reasoning,
                details={
                    "total_chunks": len(context),
                    "relevant_chunks": relevant_chunks,
                    "precision": score,
                },
            )

        except Exception as e:
            return MetricResult.from_error(f"Context precision calculation failed: {str(e)}")

    def _parse_response(self, response: str, total_chunks: int) -> tuple[float, str, list[int]]:
        """Parse judge response for precision."""
        lines = response.strip().split("\n")
        score = 0.5
        reasoning = response
        relevant_chunks = []

        for line in lines:
            line = line.strip()
            if line.upper().startswith("RELEVANT_CHUNKS:"):
                try:
                    chunks_text = line.split(":", 1)[1].strip()
                    if chunks_text.lower() != "none":
                        relevant_chunks = [int(c.strip()) for c in chunks_text.split(",") if c.strip().isdigit()]
                except (ValueError, IndexError):
                    pass
            elif line.upper().startswith("SCORE:"):
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

        return score, reasoning, relevant_chunks


class ContextRecallMetric(BaseMetric):
    """
    Context Recall metric.

    Measures whether all the information needed to answer the question
    (as defined by the ground truth) was retrieved in the context.
    """

    def __init__(self, llm_client=None):
        self.llm_client = llm_client

    @property
    def code(self) -> str:
        return "CONTEXT_RECALL"

    @property
    def requires_ground_truth(self) -> bool:
        return True

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
        """Calculate context recall score."""
        error = self._validate_inputs(generated_output, ground_truth, context)
        if error:
            return MetricResult.from_error(error)

        if not self.llm_client:
            return MetricResult.from_error("LLM client required for Context Recall metric")

        try:
            context_text = "\n\n".join(f"Context {i+1}: {c}" for i, c in enumerate(context))

            prompt = f"""You are evaluating context recall for a RAG system.

Question: {question}
Ground Truth Answer: {ground_truth}

Retrieved Context:
{context_text}

Analyze the ground truth answer and identify the key facts/information it contains.
Then check if each of these facts can be found in (or inferred from) the retrieved context.

Provide your evaluation:
KEY_FACTS: [Number of key facts in ground truth]
FACTS_FOUND: [Number of facts that can be found in context]
SCORE: [0.0 to 1.0, recall score = facts_found / key_facts]
REASONING: [Brief explanation of what information was missing, if any]"""

            response = self.llm_client.generate(prompt)
            score, reasoning, key_facts, facts_found = self._parse_response(response)

            return MetricResult(
                score=score,
                raw_score=score,
                reasoning=reasoning,
                details={
                    "key_facts": key_facts,
                    "facts_found": facts_found,
                    "recall": score,
                    "context_chunks": len(context),
                },
            )

        except Exception as e:
            return MetricResult.from_error(f"Context recall calculation failed: {str(e)}")

    def _parse_response(self, response: str) -> tuple[float, str, int, int]:
        """Parse judge response for recall."""
        lines = response.strip().split("\n")
        score = 0.5
        reasoning = response
        key_facts = 0
        facts_found = 0

        for line in lines:
            line = line.strip()
            if line.upper().startswith("KEY_FACTS:"):
                try:
                    key_facts = int(line.split(":", 1)[1].strip())
                except (ValueError, IndexError):
                    pass
            elif line.upper().startswith("FACTS_FOUND:"):
                try:
                    facts_found = int(line.split(":", 1)[1].strip())
                except (ValueError, IndexError):
                    pass
            elif line.upper().startswith("SCORE:"):
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

        return score, reasoning, key_facts, facts_found
