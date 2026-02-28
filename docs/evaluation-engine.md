# Evaluation Engine

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Next.js Frontend                               │
│                                                                             │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐ │
│  │ Dashboard │  │ Projects  │  │ Datasets  │  │ LLM       │  │ Eval Runs │ │
│  │           │  │ & Teams   │  │ & Tests   │  │ Endpoints │  │ & Results │ │
│  └───────────┘  └───────────┘  └───────────┘  └───────────┘  └───────────┘ │
│                                                                             │
│  Tech: React 18 + TypeScript + Tailwind CSS + shadcn/ui                    │
└─────────────────────────────────────┬───────────────────────────────────────┘
                                      │ REST API (JWT Auth)
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Spring Boot API                                   │
│                                                                             │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────────────────────┐ │
│  │ Auth        │  │ Project      │  │ Evaluation Engine                  │ │
│  │ Service     │  │ Service      │  │                                    │ │
│  │ (JWT)       │  │ Dataset      │  │  ┌────────────┐  ┌──────────────┐  │ │
│  └─────────────┘  │ TestCase     │  │  │ LLM Client │  │ Metric       │  │ │
│                   │ Team         │  │  │ Factory    │  │ Calculator   │  │ │
│  ┌─────────────┐  └──────────────┘  │  └─────┬──────┘  │ Factory      │  │ │
│  │ LLM         │                    │        │         └──────┬───────┘  │ │
│  │ Endpoint    │                    │        │                │          │ │
│  │ Service     │                    └────────┼────────────────┼──────────┘ │
│  └─────────────┘                             │                │            │
│                                              │                │            │
└──────────────────────────────────────────────┼────────────────┼────────────┘
                                               │                │
                                         ┌─────▼─────┐   ┌──────▼──────┐
                                         │ Your      │   │ Python      │
                                         │ Custom    │   │ Metrics     │
                                         │ LLM API   │   │ Service     │
                                         └───────────┘   └─────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|------------|
| Frontend | Next.js 14, React 18, TypeScript, Tailwind CSS, shadcn/ui |
| Backend | Spring Boot 4, Java 21, Spring Security (JWT) |
| Database | PostgreSQL 16, Flyway migrations |
| Metrics Service | Python, FastAPI, NLTK/HuggingFace |

## Frontend Pages

| Page | Route | Features |
|------|-------|----------|
| Login | `/login` | Email/password, JWT tokens |
| Dashboard | `/dashboard` | Overview, recent runs, stats |
| Projects | `/projects` | CRUD, team assignment |
| Datasets | `/projects/[id]/datasets` | Upload, manage test cases |
| LLM Endpoints | `/projects/[id]/llm-endpoints` | Register LLM, test connection |
| Evaluation Runs | `/projects/[id]/runs` | Create, monitor progress |
| Results | `/runs/[id]/results` | Scores table, charts, export |
| Settings | `/settings` | User, organization, API keys |

## Evaluation Workflows

### Workflow 1: Pre-generated (Offline)
```
Test Cases (with llmOutput) → Evaluation Engine → Results & Scores
```

### Workflow 2: Live Generation (Online)
```
Test Cases (questions only) → Your Custom LLM → Evaluation Engine → Results & Scores
```

## Database Schema

### llm_endpoints
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| project_id | UUID | FK to projects |
| name | VARCHAR(100) | Endpoint name |
| provider_type | VARCHAR(50) | OPENAI, ANTHROPIC, AZURE_OPENAI, CUSTOM |
| api_url | VARCHAR(500) | Custom API URL |
| model_name | VARCHAR(100) | Model identifier |
| encrypted_api_key | TEXT | AES-256 encrypted |
| auth_type | VARCHAR(20) | API_KEY, BEARER, BASIC, NONE |
| additional_config | JSONB | temperature, maxTokens, etc. |

### evaluation_metrics
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| code | VARCHAR(50) | BLEU, ROUGE_L, FAITHFULNESS, etc. |
| display_name | VARCHAR(100) | Human-readable name |
| metric_category | VARCHAR(30) | TRADITIONAL_NLP, RAG_SPECIFIC, LLM_AS_JUDGE |
| metric_type | VARCHAR(30) | Metric implementation type |
| requires_ground_truth | BOOLEAN | Needs reference answer |
| requires_context | BOOLEAN | Needs retrieved context |
| requires_judge_llm | BOOLEAN | Needs judge LLM |
| judge_prompt_template | TEXT | Prompt for LLM-as-Judge |

### evaluation_runs
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| project_id | UUID | FK to projects |
| dataset_id | UUID | FK to datasets (optional) |
| name | VARCHAR(200) | Run name |
| status | VARCHAR(20) | PENDING, RUNNING, COMPLETED, FAILED, CANCELLED |
| evaluation_mode | VARCHAR(20) | PRE_GENERATED, LIVE_GENERATION |
| target_llm_endpoint_id | UUID | Your LLM (for live generation) |
| judge_llm_endpoint_id | UUID | Judge LLM (for LLM-as-Judge) |
| total_test_cases | INTEGER | Total count |
| completed_test_cases | INTEGER | Progress |
| failed_test_cases | INTEGER | Failures |

### evaluation_results
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| evaluation_run_id | UUID | FK to evaluation_runs |
| test_case_id | UUID | FK to test_cases |
| generated_output | TEXT | LLM response (live generation) |
| llm_latency_ms | BIGINT | Your LLM response time |
| status | VARCHAR(20) | PENDING, COMPLETED, FAILED, SKIPPED |
| processing_time_ms | BIGINT | Total processing time |

### evaluation_scores
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| evaluation_result_id | UUID | FK to evaluation_results |
| metric_id | UUID | FK to evaluation_metrics |
| score | DOUBLE | Normalized score (0-1) |
| raw_score | DOUBLE | Original score |
| score_details | JSONB | Metric-specific breakdown |
| judge_reasoning | TEXT | LLM judge explanation |

## Metrics

| Metric | Category | Requires |
|--------|----------|----------|
| BLEU | Traditional NLP | ground_truth |
| ROUGE_L | Traditional NLP | ground_truth |
| EXACT_MATCH | Traditional NLP | ground_truth |
| CONTEXT_RELEVANCE | RAG | context, judge_llm |
| FAITHFULNESS | RAG | context, judge_llm |
| ANSWER_RELEVANCE | LLM-as-Judge | judge_llm |
| ANSWER_CORRECTNESS | LLM-as-Judge | ground_truth, judge_llm |
