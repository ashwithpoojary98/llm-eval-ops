---
sidebar_position: 3
title: Architecture
---

# Architecture

LLMOps Eval uses a **hybrid Spring Boot + FastAPI architecture** to get the best of both ecosystems.

## System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     Next.js Frontend                        │
│        React 18 + TypeScript + Tailwind + shadcn/ui        │
└──────────────────────────┬──────────────────────────────────┘
                           │ REST API (JWT)
           ┌───────────────┴───────────────┐
           ▼                               ▼
┌─────────────────────┐       ┌─────────────────────┐
│  Spring Boot API    │◄─────►│  FastAPI Engine     │
│  (Java 21)          │       │  (Python 3.11)      │
│                     │       │                     │
│ • Authentication    │       │ • LLM Clients       │
│ • Project/Dataset   │       │ • Metric Execution  │
│ • User Management   │       │ • RAGAS Integration │
│ • Results Storage   │       │ • Async Processing  │
└──────────┬──────────┘       └──────────┬──────────┘
           │                             │
           └──────────────┬──────────────┘
                          ▼
           ┌────────────────────────────┐
           │          PostgreSQL         │
           └────────────────────────────┘
```

## Why Hybrid?

| Service | Responsibility | Why This Language |
|---|---|---|
| **Spring Boot** | Auth, CRUD, business logic, multi-tenancy | Enterprise security, mature JPA ecosystem |
| **FastAPI** | LLM clients, metric execution, async jobs | Native access to Python ML libraries (RAGAS, LangChain, transformers) |
| **Next.js** | UI, real-time progress, dashboards | Type-safe React with excellent DX |

## Components

### Spring Boot API (Java 21)
- JWT authentication & authorization
- Multi-tenant project and dataset management
- LLM endpoint configuration (encrypted storage)
- Evaluation job orchestration
- Results storage and retrieval
- Email notifications

### FastAPI Evaluation Engine (Python 3.11)
- LLM client integrations (OpenAI, Anthropic, Azure, Bedrock, Vertex AI)
- Metric execution (BLEU, ROUGE, BERTScore, RAGAS)
- LLM-as-Judge framework
- Async parallel evaluation processing
- Callback to Spring Boot on completion

### Next.js Frontend (React 18)
- Project and dataset management UI
- LLM endpoint configuration
- Evaluation run configuration and monitoring
- Results dashboard with charts
- Team and user management

## Data Flow

1. User configures evaluation in the **UI**
2. UI sends request to **Spring Boot API**
3. Spring Boot persists config and triggers **FastAPI Engine**
4. FastAPI runs evaluations (parallel, with retries)
5. FastAPI sends results back to **Spring Boot** via callback
6. Spring Boot stores results in **PostgreSQL**
7. UI polls / receives updates and displays results

## Security

- JWT-based authentication with refresh tokens
- Encrypted LLM API key storage (AES-256)
- Multi-tenant data isolation
- Role-based access control (Owner, Admin, Member, Viewer)
- Domain-restricted user registration
