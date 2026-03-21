---
sidebar_position: 2
title: Quick Start
---

# Quick Start

Get LLMOps Eval running locally in minutes.

## Prerequisites

| Requirement | Version |
|---|---|
| Java | 21+ |
| Python | 3.11+ |
| Node.js | 18+ |
| PostgreSQL | 16 |
| Docker | Optional |

---

## Option 1: Docker Compose (Recommended)

The fastest way to get started.

```bash
git clone https://github.com/ashwithpoojary98/llm-eval-ops.git
cd llm-eval-ops

docker-compose up -d
```

Access:
- **Frontend**: http://localhost:3000
- **Spring Boot API**: http://localhost:8080
- **FastAPI Docs**: http://localhost:8000/docs

---

## Option 2: Manual Setup

### 1. Clone the Repository

```bash
git clone https://github.com/ashwithpoojary98/llm-eval-ops.git
cd llm-eval-ops
```

### 2. Database Setup

```bash
# Using Docker
docker run -d \
  --name postgres-llmops \
  -e POSTGRES_DB=llmevalplatform \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16
```

### 3. Spring Boot API

```bash
# Set environment variables
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/llmevalplatform
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export JWT_SECRET_KEY=your-secure-secret-key-at-least-32-chars
export LLMOPS_ADMIN_EMAIL=admin@example.com
export LLMOPS_ADMIN_PASSWORD=ChangeMe123!
export LLMOPS_ORG_NAME="My Organization"
export LLMOPS_ALLOWED_DOMAINS=example.com
export CORS_ALLOWED_ORIGINS=http://localhost:3000

# Run
./mvnw spring-boot:run
```

API starts on **http://localhost:8080**

### 4. FastAPI Evaluation Engine

```bash
cd evaluation-engine

python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

pip install -r requirements.txt

cp .env.example .env
# Edit .env with your database URL

python run.py
```

Evaluation engine starts on **http://localhost:8000**

### 5. Next.js Frontend

```bash
cd frontend

npm install

# Configure environment
echo "NEXT_PUBLIC_API_URL=http://localhost:8080/api" > .env.local

npm run dev
```

Frontend starts on **http://localhost:3000**

---

## First Evaluation

1. Open **http://localhost:3000**
2. Log in with your admin credentials
3. **Create a Project**
4. **Upload a Dataset** (CSV or JSON)
5. **Configure an LLM Endpoint** (OpenAI, Anthropic, etc.)
6. **Select Metrics** (BLEU, Faithfulness, etc.)
7. **Run Evaluation** and view results
