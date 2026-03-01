---
sidebar_position: 5
title: Evaluation Engine
---

# Evaluation Engine

The FastAPI Evaluation Engine is the Python service responsible for running all metric computations and LLM interactions.

## Overview

- **Framework**: FastAPI (Python 3.11)
- **Port**: 8000
- **API Docs**: http://localhost:8000/docs

## Supported LLM Providers

| Provider | Notes |
|---|---|
| **OpenAI** | GPT-4, GPT-3.5, and all OpenAI models |
| **Anthropic** | Claude 3 Opus, Sonnet, Haiku |
| **Azure OpenAI** | Azure-deployed OpenAI models |
| **AWS Bedrock** | Llama, Titan, and other Bedrock models |
| **Google Vertex AI** | Gemini and PaLM models |
| **Custom API** | Any REST API that accepts prompt/response |

## Key Libraries

| Library | Purpose |
|---|---|
| `ragas` | RAG evaluation metrics (Faithfulness, Context Recall, etc.) |
| `langchain` | LLM framework and chains |
| `sentence-transformers` | Semantic similarity embeddings |
| `nltk` | BLEU score calculation |
| `rouge-score` | ROUGE metric calculation |
| `bert-score` | BERTScore calculation |

## Configuration

Copy `.env.example` to `.env` and configure:

```bash
DATABASE_URL=postgresql+asyncpg://user:password@localhost:5432/llmops_eval
SPRING_BOOT_URL=http://localhost:8080
MAX_PARALLEL_EVALUATIONS=5
BATCH_SIZE=10
RETRY_ATTEMPTS=3
EMBEDDING_MODEL=all-MiniLM-L6-v2
```

## Running the Engine

```bash
cd evaluation-engine
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python run.py
```

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/health` | Health check |
| `POST` | `/api/v1/evaluate` | Trigger an evaluation |
| `GET` | `/api/v1/metrics` | List available metrics |
| `GET` | `/docs` | Swagger UI |
