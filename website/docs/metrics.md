---
sidebar_position: 4
title: Metrics Reference
---

# Metrics Reference

LLMOps Eval includes 20+ evaluation metrics across four categories.

## Traditional NLP

| Metric | Description | Use Case |
|---|---|---|
| **BLEU** | N-gram overlap between output and reference | Translation, text generation |
| **ROUGE-L** | Longest common subsequence with reference | Summarization |
| **Exact Match** | Binary match — output exactly equals reference | Q&A, classification |
| **Levenshtein** | Normalized edit distance similarity | Fuzzy matching |
| **BERTScore** | Contextual embedding similarity using BERT | Semantic quality |

## RAG-Specific

| Metric | Description |
|---|---|
| **Faithfulness** | Is the answer grounded in the retrieved context? Detects hallucinations. |
| **Answer Relevancy** | Does the answer actually address the question? |
| **Context Precision** | Is the retrieved context useful for answering? |
| **Context Recall** | Was all relevant information retrieved? |

## LLM-as-Judge

Use an LLM to evaluate another LLM's output against defined criteria.

| Metric | Description |
|---|---|
| **Relevance** | How relevant is the response to the query? |
| **Coherence** | Is the response logically structured? |
| **Fluency** | Grammar, readability, and natural language quality |
| **Toxicity** | Harmful, offensive, or biased content detection |
| **Custom** | Define your own criteria with a custom prompt |

## Performance

| Metric | Description |
|---|---|
| **Latency** | End-to-end response time in milliseconds |
| **Token Count** | Input and output token usage |
| **Cost** | Estimated API cost in USD |

---

## Example Output

```json
{
  "evaluation_id": "eval_abc123",
  "overall_score": 0.84,
  "metrics": {
    "faithfulness": 0.92,
    "answer_relevancy": 0.88,
    "context_precision": 0.79,
    "bleu": 0.65,
    "bertscore": 0.91,
    "latency_ms": 1240,
    "token_count": 312,
    "cost_usd": 0.0004
  }
}
```

## Choosing Metrics

| Use Case | Recommended Metrics |
|---|---|
| **Q&A system** | Exact Match, BLEU, Answer Relevancy |
| **RAG application** | Faithfulness, Context Precision, Context Recall, Answer Relevancy |
| **Summarization** | ROUGE-L, BERTScore, Coherence |
| **Open-ended generation** | LLM-as-Judge (Relevance, Coherence, Fluency) |
| **Production monitoring** | Latency, Token Count, Cost |
