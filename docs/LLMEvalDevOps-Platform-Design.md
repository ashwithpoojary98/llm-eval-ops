# LLMEvalDevOps Platform
## Production-Grade LLM/RAG Evaluation Platform

**Version:** 1.0
**Date:** January 2025

---

## Table of Contents

1. [Problem Statement](#1-problem-statement)
2. [Solution Overview](#2-solution-overview)
3. [Key Features](#3-key-features)
4. [System Architecture](#4-system-architecture)
5. [Domain Model](#5-domain-model)
6. [User Roles & Access Control](#6-user-roles--access-control)
7. [Core Modules](#7-core-modules)
8. [Evaluation Engine](#8-evaluation-engine)
9. [Supported Metrics](#9-supported-metrics)
10. [API Design](#10-api-design)
11. [Technology Stack](#11-technology-stack)
12. [Deployment Architecture](#12-deployment-architecture)
13. [Implementation Roadmap](#13-implementation-roadmap)

---

## 1. Problem Statement

### The Challenge

After designing an LLM application, engineering teams face significant challenges:

| Problem | Impact |
|---------|--------|
| **Custom Evaluation Frameworks** | Engineers must build evaluation systems from scratch for each project |
| **Complexity** | Evaluation requires expertise in NLP metrics, embeddings, and LLM behavior |
| **Time-Consuming** | Weeks spent building evaluation infrastructure instead of improving models |
| **Error-Prone** | Custom implementations often have bugs and edge cases |
| **Inconsistent Testing** | Each team uses different evaluation approaches |
| **Skipped Evaluations** | Due to complexity, teams often skip proper evaluation |
| **Unreliable Deployments** | Without proper evaluation, production issues go undetected |
| **No Standardization** | Difficult to compare results across projects and teams |

### Who Faces This Problem?

- **ML Engineers** building LLM-powered applications
- **Data Scientists** evaluating model performance
- **DevOps Teams** integrating evaluation into CI/CD
- **Product Teams** needing quality metrics before release
- **Non-LLM Experts** who need to evaluate LLM outputs

---

## 2. Solution Overview

### LLMEvalDevOps Platform

A **UI-driven, configuration-based evaluation platform** that enables teams to:

```
┌─────────────────────────────────────────────────────────────────┐
│                    NO CODE EVALUATION                           │
│                                                                 │
│   Define Projects → Upload Datasets → Configure Endpoints       │
│         ↓                ↓                   ↓                  │
│   Select Metrics → Run Evaluations → View Results               │
│                                                                 │
│              All through UI - No custom code needed             │
└─────────────────────────────────────────────────────────────────┘
```

### Value Proposition

| Before LLMEvalDevOps | After LLMEvalDevOps |
|---------------------|---------------------|
| Weeks to build evaluation | Minutes to configure |
| Custom code for each project | Reusable configurations |
| Manual test execution | Automated & scheduled runs |
| Inconsistent metrics | Standardized evaluation |
| No CI/CD integration | Built-in CI/CD support |
| Siloed results | Centralized dashboard |

---

## 3. Key Features

### 3.1 Project Management
- Multi-tenant organization support
- Project-based isolation
- Team collaboration with role-based access

### 3.2 Dataset Management
- Create and manage test datasets
- Support multiple formats (Q&A, RAG, Conversational)
- Import from CSV, JSON, or HuggingFace
- Version control for datasets

### 3.3 LLM Endpoint Configuration
- Connect to any LLM provider (OpenAI, Anthropic, Azure, Custom)
- Secure API key storage (encrypted)
- Health monitoring
- Rate limiting configuration

### 3.4 Evaluation Metrics
- Pre-built metrics library (20+ metrics)
- RAG-specific metrics (Faithfulness, Relevancy, Context Quality)
- LLM-as-Judge evaluations
- Custom metric support

### 3.5 Evaluation Engine
- Parallel execution for speed
- Automatic retry handling
- Progress tracking
- Cost and token tracking

### 3.6 CI/CD Integration
- API keys for pipeline integration
- Webhook triggers
- GitHub/GitLab integration
- Regression detection

### 3.7 Reporting & Analytics
- Historical trends
- Run comparisons
- Regression alerts
- Export capabilities

---

## 4. System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENTS                                         │
│     ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐           │
│     │   Web    │    │  CI/CD   │    │   API    │    │ Webhooks │           │
│     │   UI     │    │ Pipeline │    │  Client  │    │          │           │
│     └────┬─────┘    └────┬─────┘    └────┬─────┘    └────┬─────┘           │
└──────────┼───────────────┼───────────────┼───────────────┼──────────────────┘
           │               │               │               │
           └───────────────┴───────────────┴───────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         API GATEWAY / LOAD BALANCER                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                   │
           ┌───────────────────────┴───────────────────────┐
           │                                               │
           ▼                                               ▼
┌─────────────────────────────┐             ┌─────────────────────────────────┐
│      SPRING BOOT API        │             │      FASTAPI EVAL ENGINE        │
│      (Java 21)              │             │      (Python 3.11)              │
│      Port: 8080             │◄───────────►│      Port: 8000                 │
│                             │             │                                 │
│  • Authentication (JWT/SSO) │             │  • Evaluation Orchestration     │
│  • User Management          │             │  • LLM Client Integrations      │
│  • Project Management       │             │  • Metric Calculations          │
│  • Dataset Management       │             │  • RAG Evaluation (RAGAS)       │
│  • Endpoint Configuration   │             │  • LLM-as-Judge                 │
│  • Metrics Configuration    │             │  • Result Aggregation           │
│  • Schedule Management      │             │                                 │
│  • API Key Management       │             │  ┌─────────────────────────┐    │
│  • Reports & Analytics      │             │  │    Celery Workers       │    │
│                             │             │  │    (Async Processing)   │    │
└──────────────┬──────────────┘             │  └─────────────────────────┘    │
               │                             └───────────────┬─────────────────┘
               │                                             │
               └─────────────────────┬───────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            DATA LAYER                                        │
│                                                                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │   PostgreSQL    │  │     Redis       │  │    MinIO/S3     │             │
│  │                 │  │                 │  │                 │             │
│  │  • All entities │  │  • Cache        │  │  • Large        │             │
│  │  • Results      │  │  • Sessions     │  │    datasets     │             │
│  │  • Audit logs   │  │  • Job queue    │  │  • Exports      │             │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         EXTERNAL LLM PROVIDERS                               │
│                                                                             │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐  │
│  │ OpenAI  │ │Anthropic│ │  Azure  │ │ Bedrock │ │ Vertex  │ │ Custom  │  │
│  │ GPT-4   │ │ Claude  │ │ OpenAI  │ │  AWS    │ │ Google  │ │  API    │  │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Why Hybrid Architecture?

| Component | Spring Boot (Java) | FastAPI (Python) |
|-----------|-------------------|------------------|
| **Strength** | Enterprise features, Security | ML/AI ecosystem |
| **Use For** | Auth, CRUD, Business Logic | LLM calls, Evaluation |
| **Reason** | Mature security, JPA | RAGAS, LangChain, Transformers |

---

## 5. Domain Model

### Entity Relationship Diagram

```
┌──────────────────┐
│   Organization   │
│──────────────────│
│ id               │
│ name             │
│ slug             │
│ allowed_domains  │───────────────────────────────────────┐
│ sso_config       │                                       │
└────────┬─────────┘                                       │
         │ 1:N                                             │
         ▼                                                 │
┌──────────────────┐         ┌──────────────────┐         │
│      User        │         │     Project      │◄────────┘
│──────────────────│         │──────────────────│    1:N
│ id               │         │ id               │
│ email            │    N:M  │ name             │
│ password_hash    │◄───────►│ slug             │
│ role             │         │ description      │
│ organization_id  │         │ organization_id  │
└──────────────────┘         └────────┬─────────┘
                                      │
         ┌────────────────────────────┼────────────────────────────┐
         │                            │                            │
         ▼                            ▼                            ▼
┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│    Dataset       │    │   LLM Endpoint   │    │ Eval Metric      │
│──────────────────│    │──────────────────│    │──────────────────│
│ id               │    │ id               │    │ id               │
│ name             │    │ name             │    │ name             │
│ format           │    │ provider         │    │ metric_type      │
│ project_id       │    │ model_name       │    │ config           │
└────────┬─────────┘    │ api_key_encrypted│    │ weight           │
         │              │ project_id       │    │ pass_threshold   │
         │ 1:N          └──────────────────┘    │ project_id       │
         ▼                       │              └──────────────────┘
┌──────────────────┐             │                       │
│    Test Case     │             │                       │
│──────────────────│             │                       │
│ id               │             │                       │
│ input_prompt     │             │                       │
│ expected_output  │             │                       │
│ context          │             │                       │
│ ground_truth     │             │                       │
│ dataset_id       │             │                       │
└──────────────────┘             │                       │
         │                       │                       │
         │              ┌────────┴───────────────────────┘
         │              │
         ▼              ▼
┌─────────────────────────────────────┐
│          Evaluation Run             │
│─────────────────────────────────────│
│ id                                  │
│ run_number                          │
│ dataset_id ─────────────────────────┼──► Dataset
│ endpoint_id ────────────────────────┼──► LLM Endpoint
│ metric_ids[] ───────────────────────┼──► Eval Metrics
│ status (PENDING/RUNNING/COMPLETED)  │
│ trigger_type (MANUAL/CI_CD/SCHEDULED)│
│ overall_score                       │
│ total_tokens                        │
│ total_cost                          │
│ project_id                          │
└────────────────┬────────────────────┘
                 │ 1:N
                 ▼
┌─────────────────────────────────────┐
│        Evaluation Result            │
│─────────────────────────────────────│
│ id                                  │
│ run_id                              │
│ test_case_id                        │
│ llm_response                        │
│ latency_ms                          │
│ token_usage                         │
│ metric_scores {}                    │
│ passed                              │
│ error_message                       │
└─────────────────────────────────────┘
```

---

## 6. User Roles & Access Control

### Role Hierarchy

```
┌─────────────────────────────────────────────────────────────────┐
│                        ADMIN                                     │
│  • Full platform access                                         │
│  • Invite/manage users                                          │
│  • Configure SSO                                                │
│  • Manage billing                                               │
├─────────────────────────────────────────────────────────────────┤
│                      EVALUATOR                                   │
│  • Create/manage projects                                       │
│  • Configure endpoints & metrics                                │
│  • Run evaluations                                              │
│  • View all results                                             │
├─────────────────────────────────────────────────────────────────┤
│                      DEVELOPER                                   │
│  • Run evaluations                                              │
│  • View results                                                 │
│  • Use API keys                                                 │
├─────────────────────────────────────────────────────────────────┤
│                       VIEWER                                     │
│  • View projects & results                                      │
│  • Export reports                                               │
└─────────────────────────────────────────────────────────────────┘
```

### Authentication Flow (Invite-Only)

```
┌─────────────────────────────────────────────────────────────────┐
│                    INVITE-ONLY SYSTEM                            │
│                                                                 │
│  1. Bootstrap Admin (First-time setup via env vars)             │
│     └─► Admin user created on first startup                     │
│                                                                 │
│  2. Admin Invites User                                          │
│     └─► POST /api/admin/invite                                  │
│     └─► Email domain validated against allowed_domains          │
│     └─► Invitation email sent with secure token                 │
│                                                                 │
│  3. User Accepts Invitation                                     │
│     └─► Click link in email                                     │
│     └─► Set password (or link SSO)                              │
│     └─► Account activated                                       │
│                                                                 │
│  4. User Logs In                                                │
│     └─► Email/Password → JWT tokens                             │
│     └─► Or SSO (Google/Azure/Okta) → JWT tokens                 │
│                                                                 │
│  ⛔ NO PUBLIC REGISTRATION                                       │
│  ⛔ ONLY ALLOWED EMAIL DOMAINS                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. Core Modules

### 7.1 Project Module

```yaml
Purpose: Organize evaluation work by project
Features:
  - Create/update/archive projects
  - Project-level settings
  - Member management
  - Default endpoint configuration

Endpoints:
  - GET    /api/projects
  - POST   /api/projects
  - GET    /api/projects/{id}
  - PUT    /api/projects/{id}
  - DELETE /api/projects/{id}
```

### 7.2 Dataset Module

```yaml
Purpose: Manage test datasets and test cases
Features:
  - Multiple dataset formats
  - Bulk import (CSV, JSON)
  - Test case tagging
  - Version tracking

Supported Formats:
  - PROMPT_RESPONSE: Simple input/output pairs
  - RAG_QA: Question + Context + Answer
  - CONVERSATIONAL: Multi-turn dialogues
  - CLASSIFICATION: Input + Label
  - CUSTOM: User-defined schema

Endpoints:
  - GET    /api/projects/{id}/datasets
  - POST   /api/projects/{id}/datasets
  - POST   /api/projects/{id}/datasets/{id}/import
  - GET    /api/projects/{id}/datasets/{id}/test-cases
  - POST   /api/projects/{id}/datasets/{id}/test-cases/bulk
```

### 7.3 Endpoint Module

```yaml
Purpose: Configure LLM endpoints to evaluate
Features:
  - Multi-provider support
  - Secure credential storage
  - Health monitoring
  - Parameter defaults

Supported Providers:
  - OpenAI (GPT-4, GPT-3.5)
  - Anthropic (Claude 3)
  - Azure OpenAI
  - AWS Bedrock
  - Google Vertex AI
  - Ollama (Local)
  - Custom REST API

Endpoints:
  - GET    /api/projects/{id}/endpoints
  - POST   /api/projects/{id}/endpoints
  - POST   /api/projects/{id}/endpoints/{id}/test
```

### 7.4 Metrics Module

```yaml
Purpose: Configure evaluation metrics
Features:
  - Pre-built metric library
  - Custom metric support
  - Weighted scoring
  - Pass/fail thresholds

Endpoints:
  - GET    /api/projects/{id}/metrics
  - POST   /api/projects/{id}/metrics
  - GET    /api/metrics/library  # Available metrics
```

---

## 8. Evaluation Engine

### Evaluation Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         EVALUATION EXECUTION FLOW                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  REQUEST                                                                    │
│  ────────                                                                   │
│  POST /api/projects/{id}/evaluations                                        │
│  {                                                                          │
│    "datasetId": "uuid",                                                     │
│    "endpointId": "uuid",                                                    │
│    "metricIds": ["uuid1", "uuid2"]                                         │
│  }                                                                          │
│                                                                             │
│  EXECUTION STEPS                                                            │
│  ───────────────                                                            │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ 1. CREATE RUN                                                         │  │
│  │    • Generate run_number                                              │  │
│  │    • Set status = PENDING                                             │  │
│  │    • Queue for async processing                                       │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                               │                                             │
│                               ▼                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ 2. LOAD CONFIGURATION                                                 │  │
│  │    • Fetch test cases from dataset                                    │  │
│  │    • Load endpoint config & decrypt API key                           │  │
│  │    • Load metric configurations                                       │  │
│  │    • Set status = RUNNING                                             │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                               │                                             │
│                               ▼                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ 3. PARALLEL EXECUTION                                                 │  │
│  │                                                                        │  │
│  │    ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐               │  │
│  │    │ Test 1  │  │ Test 2  │  │ Test 3  │  │ Test N  │               │  │
│  │    └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘               │  │
│  │         │            │            │            │                      │  │
│  │         ▼            ▼            ▼            ▼                      │  │
│  │    ┌─────────────────────────────────────────────────────────────┐   │  │
│  │    │                    FOR EACH TEST CASE                        │   │  │
│  │    │                                                              │   │  │
│  │    │  a) Call LLM Endpoint                                        │   │  │
│  │    │     • Send prompt (with system prompt, context if RAG)       │   │  │
│  │    │     • Measure latency                                        │   │  │
│  │    │     • Track token usage                                      │   │  │
│  │    │     • Handle retries on failure                              │   │  │
│  │    │                                                              │   │  │
│  │    │  b) Evaluate Metrics                                         │   │  │
│  │    │     • Run each configured metric                             │   │  │
│  │    │     • Calculate scores                                       │   │  │
│  │    │     • Determine pass/fail                                    │   │  │
│  │    │                                                              │   │  │
│  │    │  c) Save Result                                              │   │  │
│  │    │     • Store response, scores, timing                         │   │  │
│  │    │     • Update progress                                        │   │  │
│  │    └─────────────────────────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                               │                                             │
│                               ▼                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ 4. AGGREGATE RESULTS                                                  │  │
│  │    • Calculate overall score (weighted average)                       │  │
│  │    • Aggregate metric scores                                          │  │
│  │    • Sum token usage and costs                                        │  │
│  │    • Set status = COMPLETED                                           │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                               │                                             │
│                               ▼                                             │
│  RESPONSE                                                                   │
│  ────────                                                                   │
│  {                                                                          │
│    "runId": "uuid",                                                         │
│    "runNumber": 42,                                                         │
│    "status": "COMPLETED",                                                   │
│    "overallScore": 0.85,                                                    │
│    "metricScores": {                                                        │
│      "faithfulness": 0.92,                                                  │
│      "relevancy": 0.78                                                      │
│    },                                                                       │
│    "totalTestCases": 100,                                                   │
│    "passed": 85,                                                            │
│    "failed": 15,                                                            │
│    "totalTokens": 125000,                                                   │
│    "totalCost": 2.45                                                        │
│  }                                                                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 9. Supported Metrics

### 9.1 Text-Based Metrics

| Metric | Description | Use Case |
|--------|-------------|----------|
| **Exact Match** | Binary match (case-insensitive option) | Factual Q&A |
| **Contains** | Response contains expected substring | Keyword presence |
| **Regex Match** | Pattern matching | Structured outputs |
| **BLEU** | N-gram overlap score | Translation, Generation |
| **ROUGE** | Recall-oriented overlap | Summarization |
| **Levenshtein** | Edit distance similarity | Fuzzy matching |

### 9.2 Semantic Metrics

| Metric | Description | Use Case |
|--------|-------------|----------|
| **Semantic Similarity** | Embedding cosine similarity | Meaning comparison |
| **BERTScore** | Contextual embedding similarity | Quality assessment |
| **NLI Entailment** | Natural language inference | Logical consistency |

### 9.3 RAG-Specific Metrics

| Metric | Description | Use Case |
|--------|-------------|----------|
| **Faithfulness** | Answer grounded in context | Hallucination detection |
| **Answer Relevancy** | Answer addresses the question | Quality check |
| **Context Precision** | Retrieved context usefulness | Retriever evaluation |
| **Context Recall** | All relevant info retrieved | Retriever completeness |

### 9.4 LLM-as-Judge Metrics

| Metric | Description | Judge Prompt |
|--------|-------------|--------------|
| **Relevance** | Response relevance to query | "Rate relevance 1-5..." |
| **Coherence** | Logical flow and structure | "Rate coherence 1-5..." |
| **Fluency** | Grammar and readability | "Rate fluency 1-5..." |
| **Toxicity** | Harmful content detection | "Check for toxicity..." |
| **Custom** | User-defined criteria | User-provided prompt |

### 9.5 Performance Metrics

| Metric | Description | Measurement |
|--------|-------------|-------------|
| **Latency** | Response time | Milliseconds |
| **Token Count** | Input/output tokens | Token count |
| **Cost** | API cost | USD |

---

## 10. API Design

### Authentication Endpoints

```
POST /api/auth/login              # Email/password login → JWT
POST /api/auth/logout             # Revoke refresh token
POST /api/auth/refresh            # Refresh access token
GET  /api/auth/sso/{provider}     # Initiate SSO
POST /api/auth/sso/{provider}/callback  # SSO callback
```

### Admin Endpoints

```
POST   /api/admin/invite          # Invite user (domain validated)
GET    /api/admin/invitations     # List pending invitations
DELETE /api/admin/invitations/{id}# Revoke invitation
GET    /api/admin/users           # List organization users
PATCH  /api/admin/users/{id}/role # Update user role
DELETE /api/admin/users/{id}      # Deactivate user
```

### Project Endpoints

```
GET    /api/projects              # List user's projects
POST   /api/projects              # Create project
GET    /api/projects/{id}         # Get project details
PUT    /api/projects/{id}         # Update project
DELETE /api/projects/{id}         # Archive project
```

### Dataset Endpoints

```
GET    /api/projects/{id}/datasets              # List datasets
POST   /api/projects/{id}/datasets              # Create dataset
POST   /api/projects/{id}/datasets/{id}/import  # Import test cases
GET    /api/projects/{id}/datasets/{id}/export  # Export dataset
```

### Test Case Endpoints

```
GET    /api/projects/{id}/datasets/{id}/test-cases       # List
POST   /api/projects/{id}/datasets/{id}/test-cases       # Create
POST   /api/projects/{id}/datasets/{id}/test-cases/bulk  # Bulk create
PUT    /api/projects/{id}/datasets/{id}/test-cases/{id}  # Update
DELETE /api/projects/{id}/datasets/{id}/test-cases/{id}  # Delete
```

### Endpoint Configuration

```
GET    /api/projects/{id}/endpoints           # List endpoints
POST   /api/projects/{id}/endpoints           # Create endpoint
POST   /api/projects/{id}/endpoints/{id}/test # Test connectivity
PUT    /api/projects/{id}/endpoints/{id}      # Update
DELETE /api/projects/{id}/endpoints/{id}      # Delete
```

### Evaluation Endpoints

```
GET    /api/projects/{id}/evaluations              # List runs
POST   /api/projects/{id}/evaluations              # Start evaluation
GET    /api/projects/{id}/evaluations/{id}         # Get run status
GET    /api/projects/{id}/evaluations/{id}/results # Get results
POST   /api/projects/{id}/evaluations/{id}/cancel  # Cancel run
GET    /api/projects/{id}/evaluations/{id}/compare/{otherId}  # Compare
```

### CI/CD Integration

```
POST   /api/api-keys              # Create API key
GET    /api/api-keys              # List API keys
DELETE /api/api-keys/{id}         # Revoke API key

# Trigger via API key (X-API-Key header)
POST   /api/v1/evaluations/trigger  # Trigger evaluation from CI/CD
```

---

## 11. Technology Stack

### Backend Services

| Component | Technology | Version | Purpose |
|-----------|------------|---------|---------|
| **API Server** | Spring Boot | 4.0.2 | Main API, Auth, CRUD |
| **Eval Engine** | FastAPI | 0.109+ | Evaluation, LLM calls |
| **Language** | Java | 21 | Spring Boot |
| **Language** | Python | 3.11+ | FastAPI, ML libs |
| **ORM** | Spring Data JPA | - | Java persistence |
| **ORM** | SQLAlchemy | 2.0+ | Python persistence |
| **Security** | Spring Security | 6.x | Authentication |
| **JWT** | jjwt / python-jose | - | Token handling |

### Data Layer

| Component | Technology | Version | Purpose |
|-----------|------------|---------|---------|
| **Database** | PostgreSQL | 16 | Primary data store |
| **Cache** | Redis | 7 | Caching, sessions |
| **Queue** | Redis/Celery | - | Job queue |
| **Storage** | MinIO/S3 | - | Large files |

### ML/AI Libraries (Python)

| Library | Purpose |
|---------|---------|
| **openai** | OpenAI API client |
| **anthropic** | Anthropic API client |
| **ragas** | RAG evaluation metrics |
| **langchain** | LLM framework |
| **sentence-transformers** | Embeddings |
| **nltk** | BLEU score |
| **rouge-score** | ROUGE metrics |
| **tiktoken** | Token counting |

### DevOps

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Containers** | Docker | Containerization |
| **Orchestration** | Docker Compose / K8s | Deployment |
| **CI/CD** | GitHub Actions | Automation |
| **Monitoring** | Prometheus + Grafana | Metrics |

---

## 12. Deployment Architecture

### Docker Compose (Development/Small Scale)

```yaml
Services:
  - postgres (Database)
  - redis (Cache + Queue)
  - spring-api (Main API)
  - fastapi (Eval Engine)
  - celery-worker (Async Jobs)
  - celery-beat (Scheduler)
```

### Kubernetes (Production)

```
┌─────────────────────────────────────────────────────────────────┐
│                      KUBERNETES CLUSTER                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    INGRESS CONTROLLER                    │   │
│  │                    (nginx / traefik)                     │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│              ┌───────────────┴───────────────┐                 │
│              ▼                               ▼                  │
│  ┌─────────────────────┐       ┌─────────────────────┐        │
│  │   Spring API        │       │   FastAPI Engine    │        │
│  │   Deployment        │       │   Deployment        │        │
│  │   (3 replicas)      │       │   (3 replicas)      │        │
│  │   HPA: 3-10         │       │   HPA: 3-20         │        │
│  └─────────────────────┘       └─────────────────────┘        │
│                                          │                     │
│                              ┌───────────┴───────────┐        │
│                              ▼                       ▼         │
│                  ┌─────────────────┐   ┌─────────────────┐    │
│                  │ Celery Workers  │   │ Celery Beat     │    │
│                  │ Deployment      │   │ Deployment      │    │
│                  │ (5-50 replicas) │   │ (1 replica)     │    │
│                  └─────────────────┘   └─────────────────┘    │
│                                                                │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                    STATEFUL SERVICES                     │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │  │
│  │  │ PostgreSQL   │  │ Redis        │  │ MinIO        │   │  │
│  │  │ StatefulSet  │  │ StatefulSet  │  │ StatefulSet  │   │  │
│  │  │ (HA with     │  │ (Sentinel)   │  │ (Distributed)│   │  │
│  │  │  replicas)   │  │              │  │              │   │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘   │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 13. Implementation Roadmap

### Phase 1: Foundation (Weeks 1-2)
```
✓ Spring Boot project setup
✓ Database schema design
□ Flyway migrations
□ User entity & repository
□ JWT authentication
□ Admin bootstrap
□ Invite-only signup
□ Domain validation
```

### Phase 2: Core Entities (Weeks 3-4)
```
□ Organization management
□ Project CRUD
□ Project membership
□ Dataset management
□ Test case CRUD
□ Bulk import (CSV/JSON)
□ LLM endpoint configuration
□ API key encryption
```

### Phase 3: Evaluation Engine (Weeks 5-7)
```
□ FastAPI project setup
□ LLM client implementations
  □ OpenAI
  □ Anthropic
  □ Azure OpenAI
  □ Custom API
□ Basic metrics
  □ Exact match
  □ Contains
  □ BLEU/ROUGE
□ Evaluation orchestrator
□ Celery worker setup
□ Result storage
```

### Phase 4: RAG Evaluation (Weeks 8-9)
```
□ RAGAS integration
□ Faithfulness metric
□ Answer relevancy
□ Context precision/recall
□ LLM-as-Judge framework
□ Custom judge prompts
```

### Phase 5: CI/CD & Scheduling (Weeks 10-11)
```
□ API key management
□ CI/CD trigger endpoint
□ Scheduled evaluations
□ Webhook notifications
□ GitHub/GitLab integration
```

### Phase 6: Reporting & Polish (Weeks 12-14)
```
□ Dashboard UI
□ Trend analysis
□ Run comparison
□ Regression detection
□ Export capabilities
□ Documentation
□ Testing & QA
```

---

## Appendix A: Database Schema Summary

| Table | Purpose | Key Fields |
|-------|---------|------------|
| organizations | Multi-tenant support | name, allowed_domains, sso_config |
| users | User accounts | email, role, organization_id |
| user_invitations | Invite workflow | email, token_hash, expires_at |
| refresh_tokens | JWT refresh | user_id, token_hash, expires_at |
| projects | Evaluation projects | name, organization_id |
| project_members | Access control | project_id, user_id, role |
| datasets | Test datasets | name, format, project_id |
| test_cases | Individual tests | input_prompt, expected_output, context |
| llm_endpoints | LLM configurations | provider, model, api_key_encrypted |
| evaluation_metrics | Metric configs | metric_type, config, weight |
| evaluation_runs | Evaluation executions | status, overall_score, dataset_id |
| evaluation_results | Per-test results | llm_response, metric_scores, passed |
| evaluation_schedules | Cron schedules | cron_expression, dataset_id |
| api_keys | CI/CD integration | key_hash, scopes, project_id |

---

## Appendix B: Environment Variables

```bash
# Database
DATABASE_URL=postgresql://user:pass@host:5432/llmevalplatform
DATABASE_POOL_SIZE=20

# Redis
REDIS_URL=redis://localhost:6379/0
CELERY_BROKER_URL=redis://localhost:6379/1

# Security
JWT_SECRET_KEY=your-256-bit-secret
ENCRYPTION_KEY=your-encryption-key

# Admin Bootstrap
LLMOPS_ADMIN_EMAIL=admin@company.com
LLMOPS_ADMIN_PASSWORD=SecurePassword123!
LLMOPS_ALLOWED_DOMAINS=company.com,subsidiary.com

# SSO (Optional)
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
AZURE_CLIENT_ID=...
AZURE_CLIENT_SECRET=...
AZURE_TENANT_ID=...

# Service Communication
SPRING_API_URL=http://localhost:8080
FASTAPI_URL=http://localhost:8000
INTERNAL_SERVICE_KEY=...
```

---

## Appendix C: Sample API Requests

### Start Evaluation
```bash
curl -X POST https://api.llmeval.io/api/projects/{id}/evaluations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "datasetId": "dataset-uuid",
    "endpointId": "endpoint-uuid",
    "metricIds": ["metric-1-uuid", "metric-2-uuid"]
  }'
```

### CI/CD Trigger
```bash
curl -X POST https://api.llmeval.io/api/v1/evaluations/trigger \
  -H "X-API-Key: llmeval_abc123..." \
  -H "Content-Type: application/json" \
  -d '{
    "projectId": "project-uuid",
    "datasetId": "dataset-uuid",
    "endpointId": "endpoint-uuid",
    "ciMetadata": {
      "gitCommit": "abc123",
      "gitBranch": "main",
      "prNumber": 42
    }
  }'
```

---

**Document End**

*LLMEvalDevOps Platform - Making LLM Evaluation Accessible to Everyone*
