---
sidebar_position: 1
title: Introduction
---

# LLMOps Eval Platform

> **Production-grade LLM/RAG evaluation platform with UI-driven configuration, multi-provider support, and comprehensive metrics.**

## The Problem

After building an LLM application, teams struggle with:

- **Weeks spent** building custom evaluation frameworks from scratch
- **Complexity** requiring expertise in NLP metrics, embeddings, and LLM behavior
- **Inconsistent testing** across different projects and teams
- **Skipped evaluations** due to implementation difficulty
- **Unreliable deployments** without proper quality gates

## The Solution

**LLMOps Eval** is a **no-code evaluation platform** that lets you:

```
Define Projects → Upload Datasets → Configure Endpoints → Select Metrics → Run Evaluations → View Results
```

**All through a UI — no custom code needed.**

---

## Key Features

### Core Capabilities

- **Multi-Tenant Architecture** — Organizations, projects, and team-based access control
- **Dataset Management** — Create, import (CSV/JSON), and version test datasets
- **LLM Provider Support** — OpenAI, Anthropic, Azure OpenAI, AWS Bedrock, Google Vertex AI, Custom APIs
- **20+ Evaluation Metrics** — Traditional NLP, RAG-specific, and LLM-as-Judge
- **Parallel Execution** — Fast evaluation with automatic retry handling
- **CI/CD Integration** — API keys, webhooks, GitHub/GitLab integration
- **Cost & Token Tracking** — Monitor usage and costs across evaluations
- **Regression Detection** — Compare runs and detect quality degradation

### Supported Metrics

| Category | Metrics |
|---|---|
| **Traditional NLP** | BLEU, ROUGE, Exact Match, Levenshtein, BERTScore |
| **RAG-Specific** | Faithfulness, Answer Relevancy, Context Precision, Context Recall |
| **LLM-as-Judge** | Relevance, Coherence, Fluency, Toxicity, Custom criteria |
| **Performance** | Latency, Token Count, Cost |

---

## Technology Stack

| Component | Technology |
|---|---|
| Backend API | Spring Boot 3.x (Java 21) |
| Evaluation Engine | FastAPI (Python 3.11) |
| Frontend | Next.js 14 (React 18) |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |

---

## Next Steps

- [Quick Start](./quick-start) — Get up and running in minutes
- [Architecture](./architecture) — Understand how it all fits together
- [Metrics Reference](./metrics) — Explore all available metrics
