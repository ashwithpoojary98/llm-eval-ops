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
| Redis | 7 |
| Docker | Optional |

---

## Option 1: Docker Compose (Recommended)

The fastest way to get started.

```bash
git clone https://github.com/ashwithpoojary98/llmops-eval.git
cd llmops-eval

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
git clone https://github.com/ashwithpoojary98/llmops-eval.git
cd llmops-eval
```

### 2. Database Setup

```bash
# Using Docker
docker run -d \
  --name postgres-llmops \
  -e POSTGRES_DB=llmops_eval \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16
```

### 3. Spring Boot API

```bash
# Set environment variables
export DATABASE_URL=jdbc:postgresql://localhost:5432/llmops_eval
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
export JWT_SECRET_KEY=your-secure-secret-key
export LLMOPS_ADMIN_EMAIL=admin@example.com
export LLMOPS_ADMIN_PASSWORD=ChangeMe123!

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
