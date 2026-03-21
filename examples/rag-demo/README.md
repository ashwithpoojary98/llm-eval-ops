# RAG Demo — TechCorp FAQ Bot

A working RAG application that evaluates itself using LLMOps Eval. Use this to verify the platform is running correctly and to see real evaluation results.

## What it does

```
sample_docs/company_faq.txt
        │
        ▼  (paragraph chunking)
   Document Store (12 chunks)
        │
        ▼  (sentence-transformers embedding, runs locally)
   Vector Index
        │
   Question ──► Retrieve top-3 chunks ──► GPT-4o-mini ──► Answer
                                                   │
                                        LLMOps Eval Platform
                                        (25 metrics scored)
                                                   │
                                           Rich Results Table
```

## Setup

```bash
cd examples/rag-demo
python -m venv .venv

# Windows
.venv\Scripts\activate

# macOS/Linux
source .venv/bin/activate

pip install -r requirements.txt
cp .env.example .env
# Edit .env — at minimum set LLMOPS_TOKEN
```

## Get your LLMOPS_TOKEN

```bash
# Register (first time only)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@test.com","password":"Demo1234!","firstName":"Demo","lastName":"User","organizationName":"Demo Org"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@test.com","password":"Demo1234!"}' | jq -r '.token'
```

Paste the token into `.env`.

## Run (two modes)

### Mode A — Mock answers (free, no OpenAI key, tests the eval pipeline)

```bash
python evaluate_rag.py --mock
```

Uses keyword-based mock answers. LLM-judge metrics (FAITHFULNESS, RELEVANCE, etc.) default to 0.5 since no judge LLM is configured, but all NLP metrics (ROUGE, BLEU, Semantic Similarity) run fully and show real scores.

### Mode B — Real OpenAI answers + judge scoring

```bash
# Set OPENAI_API_KEY in .env first
python evaluate_rag.py
```

Cost estimate: ~$0.02-0.05 per full run with GPT-4o-mini as both generator and judge.

### Custom metrics

```bash
python evaluate_rag.py --metrics ROUGE_L BLEU SEMANTIC_SIMILARITY BERTSCORE
```

## Expected output

```
============================================================
  RAG Evaluation Demo — LLMOps Eval Platform
============================================================
Mode: OPENAI sk-proj-...
Test cases: 12
Metrics: ROUGE_L, SEMANTIC_SIMILARITY, FAITHFULNESS, CONTEXT_RECALL, RELEVANCE, ANSWER_CORRECTNESS

=== Initializing RAG Pipeline ===
Loaded 28 chunks from sample_docs/
Building document index...
Pipeline ready.

Running test cases through RAG pipeline...
  [rag_001] Q: How do I cancel my subscription?...
             A: Go to Settings > Subscription > Cancel Plan...

  ...

Submitting to LLMOps Eval platform...
Using project: <project-uuid>
Evaluation submitted. Run ID: rag-demo-20240315-143022
Waiting for results.....................

╭─────────────────────────────────────────────────────────╮
│                  LLMOps Eval Results                    │
│  Overall Score: 0.821                                   │
│  Completed: 12 / 12   Cost: $0.0231                     │
╰─────────────────────────────────────────────────────────╯

  Metric Summaries
  ┌─────────────────────┬───────────┬───────┬───────┬──────────┬───────────┐
  │ Metric              │ Avg Score │   Min │   Max │ PassRate │ Threshold │
  ├─────────────────────┼───────────┼───────┼───────┼──────────┼───────────┤
  │ ROUGE_L             │     0.581 │ 0.241 │ 0.893 │      83% │      0.35 │
  │ SEMANTIC_SIMILARITY │     0.832 │ 0.612 │ 0.971 │      92% │      0.75 │
  │ FAITHFULNESS        │     0.891 │ 0.310 │ 0.990 │      83% │      0.80 │
  │ CONTEXT_RECALL      │     0.823 │ 0.510 │ 0.980 │      83% │      0.75 │
  │ RELEVANCE           │     0.879 │ 0.720 │ 0.970 │     100% │      0.78 │
  │ ANSWER_CORRECTNESS  │     0.801 │ 0.410 │ 0.960 │      83% │      0.75 │
  └─────────────────────┴───────────┴───────┴───────┴──────────┴───────────┘

Hallucination Test (rag_012): FAITHFULNESS = 0.07 ✅ Correctly detected

Full results: http://localhost:3000/evaluations/rag-demo-20240315-143022
```

## What to look for

| Result | What it means |
|--------|---------------|
| `FAITHFULNESS = 0.07` on hallucination test | RAG correctly said "I don't have that info" — the eval caught it |
| `SEMANTIC_SIMILARITY > 0.80` | Answers are semantically close to ground truth |
| `ROUGE_L < 0.35` on any case | Answer uses different wording from ground truth (not necessarily wrong) |
| `CONTEXT_RECALL < 0.75` | Relevant context chunks weren't retrieved — tuning `top_k` or embedding model may help |

## What to try next

1. **Increase `top_k`** in `rag_pipeline.py` (line where `RAGPipeline` is created) and see if `CONTEXT_RECALL` improves
2. **Add more documents** to `sample_docs/` — any `.txt` file is auto-loaded
3. **Set a `passThreshold`** and hook a webhook to alert when quality drops
4. **Compare embedding models** — run twice with `all-MiniLM-L6-v2` vs `all-mpnet-base-v2` and compare scores in the UI
5. **Swap the LLM** from GPT-4o-mini to Claude 3.5 Haiku and compare scores + cost
