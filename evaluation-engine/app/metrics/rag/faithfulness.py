"""
Faithfulness metric - measures if the answer is grounded in context.
"""
from typing import Optional

from app.metrics.base import BaseMetric, MetricResult


class FaithfulnessMetric(BaseMetric):
    """
    Faithfulness metric for RAG evaluation.

    Measures whether the generated answer is grounded in the provided context,
    detecting potential hallucinations where the LLM generates information
    not supported by the retrieved documents.

    Requires an LLM judge to evaluate faithfulness.
    """

    def __init__(self, llm_client=None):
        self.llm_client = llm_client

    @property
    def code(self) -> str:
        return "FAITHFULNESS"

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
        """Calculate faithfulness score using LLM judge."""
        error = self._validate_inputs(generated_output, ground_truth, context)
        if error:
            return MetricResult.from_error(error)

        if not self.llm_client:
            return MetricResult.from_error("LLM client required for Faithfulness metric")

        try:
            context_text = "\n\n".join(f"Context {i+1}: {c}" for i, c in enumerate(context))

            prompt = f"""You are evaluating whether an answer is faithful to the provided context.

Question: {question}

Context:
{context_text}

Answer: {generated_output}

Evaluate whether the answer is completely supported by the context. Check for:
1. Claims in the answer that are not found in the context (hallucinations)
2. Information that contradicts the context
3. Accurate representation of context information

Provide your evaluation in the following format:
SCORE: [0.0 to 1.0, where 1.0 means completely faithful]
REASONING: [Brief explanation of your score]

Be strict - any hallucination or unsupported claim should significantly lower the score."""

            response = self.llm_client.generate(prompt)
            score, reasoning = self._parse_judge_response(response)

            return MetricResult(
                score=score,
                raw_score=score,
                reasoning=reasoning,
                details={
                    "context_chunks": len(context),
                    "answer_length": len(generated_output),
                },
            )

        except Exception as e:
            return MetricResult.from_error(f"Faithfulness calculation failed: {str(e)}")

    def _parse_judge_response(self, response: str) -> tuple[float, str]:
        """Parse LLM judge response to extract score and reasoning."""
        lines = response.strip().split("\n")
        score = 0.5  # Default score
        reasoning = response

        for line in lines:
            line = line.strip()
            if line.upper().startswith("SCORE:"):
                try:
                    score_text = line.split(":", 1)[1].strip()
                    # Handle formats like "0.8" or "0.8/1.0" or "8/10"
                    if "/" in score_text:
                        parts = score_text.split("/")
                        score = float(parts[0]) / float(parts[1])
                    else:
                        score = float(score_text)
                    score = max(0.0, min(1.0, score))  # Clamp to 0-1
                except (ValueError, IndexError):
                    pass
            elif line.upper().startswith("REASONING:"):
                reasoning = line.split(":", 1)[1].strip()

        return score, reasoning
