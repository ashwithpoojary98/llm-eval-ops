# LLMOps Eval — Architecture Overview

## System Architecture

```mermaid
graph TB
    subgraph Client["Client Layer"]
        Browser["Browser / API Consumer"]
    end

    subgraph Frontend["Frontend (Next.js 14)"]
        UI["React UI Pages"]
        AdminUI["Admin UI\n(Settings · Health · Webhooks)"]
    end

    subgraph Backend["Backend (Spring Boot 4 · Java 21)"]
        direction TB
        AuthAPI["Auth & Multi-Tenant API"]
        EvalAPI["Evaluation Run API"]
        DatasetAPI["Dataset & Prompt API"]
        HealthAPI["LLM Health Monitor API"]
        WebhookAPI["Webhook Management API"]
        AdminAPI["Admin Settings API"]

        Scheduler["Scheduler\n(Health probe · 5 min\nWebhook retry · 1 min)"]
        WebhookDispatch["Webhook Dispatch\n(HMAC-SHA256 signing\nAsync delivery)"]
        EmailService["Email Service\n(Dynamic SMTP from DB)"]
    end

    subgraph EvalEngine["Evaluation Engine (Python FastAPI)"]
        direction TB
        EvalRouter["Evaluation Router"]
        EvalService["Evaluation Service\n(parallel batches · semaphore)"]
        MetricRegistry["Metric Registry\n(25 metrics across 5 categories)"]

        subgraph Metrics["Metric Categories"]
            NLP["Traditional NLP\nBLEU · ROUGE · METEOR"]
            Semantic["Semantic\nCosine Similarity · BERTScore"]
            RAG["RAG-Specific\nFaithfulness · Context Recall"]
            Judge["LLM-as-Judge\nRelevance · Correctness · Fluency"]
            Perf["Performance\nLatency · Token Cost"]
        end
    end

    subgraph Storage["Storage Layer"]
        PG[("PostgreSQL\n(shared DB)")]
    end

    subgraph LLMs["External LLM Providers"]
        OpenAI["OpenAI"]
        Anthropic["Anthropic"]
        Azure["Azure OpenAI"]
        Custom["Custom / Local\n(Ollama · vLLM)"]
    end

    Browser --> Frontend
    Frontend --> Backend

    Backend --> EvalEngine
    Backend --> PG
    Backend --> LLMs

    EvalEngine --> PG
    EvalEngine --> LLMs

    Scheduler --> LLMs
    WebhookDispatch --> Browser
```

---

## Request Flow: Evaluation Run

```mermaid
sequenceDiagram
    participant Client
    participant SpringBoot as Spring Boot API
    participant EvalEngine as Evaluation Engine
    participant DB as PostgreSQL
    participant LLM as Target / Judge LLM
    participant Webhook as Webhook Receiver

    Client->>SpringBoot: POST /api/evaluations (run config)
    SpringBoot->>DB: Save EvaluationRun (PENDING)
    SpringBoot->>EvalEngine: POST /evaluate (test cases + metrics)
    EvalEngine-->>SpringBoot: 202 task_id accepted

    SpringBoot-->>Client: 202 evaluation_run_id

    Note over EvalEngine: Background task starts

    EvalEngine->>DB: Update task → RUNNING

    loop Batch (parallel with semaphore)
        alt LIVE_GENERATION mode
            EvalEngine->>LLM: generate_with_metadata(prompt)
            LLM-->>EvalEngine: response + tokens + cost
        end
        EvalEngine->>LLM: judge LLM metrics (async thread pool)
        EvalEngine->>DB: Store EvaluationResult + MetricScores
    end

    EvalEngine->>DB: Update task → COMPLETED (overall_score)
    EvalEngine->>SpringBoot: POST callback_url (status + scores)

    SpringBoot->>DB: Update EvaluationRun status
    SpringBoot->>Webhook: Fire EVALUATION_COMPLETED event (HMAC signed)
    Webhook-->>SpringBoot: 200 OK
```

---

## LLM Health Monitoring Flow

```mermaid
sequenceDiagram
    participant Scheduler as Spring Boot Scheduler
    participant LLM as LLM Endpoints
    participant DB as PostgreSQL
    participant Webhook as Registered Webhooks

    loop Every 5 minutes
        Scheduler->>DB: Load all active endpoints
        loop Per endpoint (parallel)
            Scheduler->>LLM: Provider-specific health probe
            LLM-->>Scheduler: HTTP response + latency
            Scheduler->>DB: Write LlmHealthRecord
            Scheduler->>DB: Update LlmEndpointHealthStatus\n(24h uptime %, consecutive failures)
        end

        alt Status changed (UP→DOWN etc.)
            Scheduler->>Webhook: Fire LLM_HEALTH_DOWN / RECOVERED event
        end
    end
```

---

## Component Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        LLMOps Eval Platform                            │
│                                                                        │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    Next.js 14 Frontend                          │   │
│  │  /dashboard  /evaluations  /datasets  /llm-endpoints            │   │
│  │  /admin/settings  /admin/health  /admin/webhooks                │   │
│  └─────────────────┬───────────────────────────────────────────────┘   │
│                    │ REST / JSON                                        │
│  ┌─────────────────▼───────────────────────────────────────────────┐   │
│  │                Spring Boot 4 Backend (Port 8080)                │   │
│  │                                                                 │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │   │
│  │  │  Auth + RBAC │  │  Evaluation  │  │  LLM Health Monitor  │  │   │
│  │  │  Multi-tenant│  │  Run Manager │  │  Scheduler + Probes  │  │   │
│  │  └──────────────┘  └──────┬───────┘  └──────────────────────┘  │   │
│  │                           │                                     │   │
│  │  ┌──────────────┐  ┌──────▼───────┐  ┌──────────────────────┐  │   │
│  │  │  Dataset /   │  │  Webhook     │  │  Admin Settings      │  │   │
│  │  │  Prompt Mgmt │  │  Dispatch    │  │  Dynamic SMTP Config │  │   │
│  │  └──────────────┘  └──────────────┘  └──────────────────────┘  │   │
│  │                                                                 │   │
│  │  ┌──────────────────────────────────────────────────────────┐   │   │
│  │  │              PostgreSQL (Flyway migrations)               │   │   │
│  │  └──────────────────────────────────────────────────────────┘   │   │
│  └─────────────────┬───────────────────────────────────────────────┘   │
│                    │ HTTP (internal)                                    │
│  ┌─────────────────▼───────────────────────────────────────────────┐   │
│  │          Python FastAPI Evaluation Engine (Port 8000)           │   │
│  │                                                                 │   │
│  │  ┌──────────────────────────────────────────────────────────┐   │   │
│  │  │                    Metric Registry                       │   │   │
│  │  │  Traditional NLP │ Semantic │ RAG │ Judge │ Performance  │   │   │
│  │  └──────────────────────────────────────────────────────────┘   │   │
│  │                                                                 │   │
│  │  asyncio.gather() parallel batches · semaphore concurrency     │   │
│  │  asyncio.to_thread() for sync LLM SDK calls                    │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## How LLMOps Eval Differs from Other Tools

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                     Competitive Differentiation Matrix                         │
├──────────────────────┬─────────────┬───────────────┬────────────┬─────────────┤
│ Feature              │ LLMOps Eval │  LangSmith    │ PromptLayer│   W&B Weave │
├──────────────────────┼─────────────┼───────────────┼────────────┼─────────────┤
│ Fully self-hosted    │      ✅      │  ❌ Cloud only │  ❌ Cloud  │ Partial     │
│ Open source (MIT)    │      ✅      │  ❌ Proprietary│  ❌        │ ❌          │
│ 25 built-in metrics  │      ✅      │  Custom only  │  Limited   │ Custom only │
│ LLM Health Monitor   │      ✅      │  ❌            │  ❌        │  ❌         │
│ Webhook system       │      ✅      │  ❌            │  ❌        │  ❌         │
│ Multi-tenant org/proj│      ✅      │  Workspace    │  Limited   │  Teams      │
│ Dynamic email config │      ✅      │  ❌            │  ❌        │  ❌         │
│ K8s-ready (Kustomize)│      ✅      │  N/A          │  N/A       │  N/A        │
│ RAG-specific metrics │      ✅      │  Partial      │  ❌        │  Partial    │
│ BERTScore built-in   │      ✅      │  ❌            │  ❌        │  ❌         │
│ LIVE_GENERATION mode │      ✅      │  ✅            │  ✅        │  ✅         │
│ Async batch parallel │      ✅      │  N/A          │  N/A       │  N/A        │
│ No per-seat pricing  │      ✅      │  ❌            │  ❌        │  ❌         │
└──────────────────────┴─────────────┴───────────────┴────────────┴─────────────┘
```

### Key Differentiators in Detail

**1. Self-Hosted + Open Source**
LangSmith, PromptLayer, and W&B Weave are SaaS products that send your prompts and outputs to their cloud. LLMOps Eval runs entirely in your infrastructure — critical for regulated industries (healthcare, finance, government) where data cannot leave the perimeter.

**2. Built-in LLM Health Monitoring**
No other evaluation platform includes an active health monitoring layer that probes LLM endpoints every 5 minutes, tracks 24-hour uptime percentages, detects degradation, and fires webhooks on status changes. This turns the platform from a passive evaluation tool into an operational observability layer.

**3. 25 Metrics Out of the Box**
| Category | Metrics |
|---|---|
| Traditional NLP | BLEU, ROUGE-1/2/L, METEOR, CER, WER |
| Semantic | Cosine Similarity (sentence-transformers), BERTScore |
| RAG-Specific | Faithfulness, Context Precision, Context Recall, Answer Relevance |
| LLM-as-Judge | Relevance, Coherence, Fluency, Toxicity, Answer Correctness, Custom |
| Performance | Latency, Token count, Cost estimation |

**4. Webhook System with HMAC Signing**
12 event types with HMAC-SHA256 signature verification — enabling integrations with Slack, PagerDuty, CI/CD pipelines, or any HTTP endpoint. Automatic retry with exponential backoff, auto-disable after 10 consecutive failures.

**5. Multi-Tenant with Dynamic Admin Config**
Organizations can configure their own SMTP servers, platform branding, and webhook endpoints through the UI — no server restart required. Built for MSPs and SaaS builders who serve multiple customers from one deployment.
