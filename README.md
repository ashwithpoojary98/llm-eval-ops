# LLMOps Eval Platform

> **Production-grade LLM/RAG evaluation platform with UI-driven configuration, multi-provider support, and comprehensive metrics**

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.11+-blue.svg)](https://www.python.org/)
[![Next.js](https://img.shields.io/badge/Next.js-14-black.svg)](https://nextjs.org/)

---

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

**All through a UI - No custom code needed.**

---

## Key Features

### 🎯 Core Capabilities

- **Multi-Tenant Architecture** - Organizations, projects, and team-based access control
- **Dataset Management** - Create, import (CSV/JSON), and version test datasets
- **LLM Provider Support** - OpenAI, Anthropic, Azure OpenAI, AWS Bedrock, Google Vertex AI, Custom APIs
- **20+ Evaluation Metrics** - Traditional NLP, RAG-specific, and LLM-as-Judge
- **Parallel Execution** - Fast evaluation with automatic retry handling
- **Cost & Token Tracking** - Monitor usage and costs across evaluations
- **Regression Detection** - Compare runs and detect quality degradation

### 📊 Supported Metrics

| Category | Metrics |
|----------|---------|
| **Traditional NLP** | BLEU, ROUGE, Exact Match, Levenshtein, BERTScore |
| **RAG-Specific** | Faithfulness, Answer Relevancy, Context Precision, Context Recall |
| **LLM-as-Judge** | Relevance, Coherence, Fluency, Toxicity, Custom criteria |
| **Performance** | Latency, Token Count, Cost |

---

## Architecture

### Hybrid Spring Boot + FastAPI

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
           │      PostgreSQL + Redis     │
           └────────────────────────────┘
```

**Why Hybrid?**
- **Spring Boot**: Enterprise security, mature JPA, robust business logic
- **FastAPI**: Native ML/AI libraries (RAGAS, LangChain, transformers)

---

## Quick Start

### Prerequisites

- Java 21+
- Python 3.11+
- Node.js 18+
- PostgreSQL 16
- Docker (optional)

### 1. Clone the Repository

```bash
git clone https://github.com/ashwithpoojary98/llm-eval-ops.git
cd llm-eval-ops
```

### 2. Database Setup

```bash
# Create PostgreSQL database
createdb llmops_eval

# Or using Docker
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
# Configure application.properties
cat > src/main/resources/application.properties <<EOF
spring.datasource.url=jdbc:postgresql://localhost:5432/llmops_eval
spring.datasource.username=postgres
spring.datasource.password=postgres

# JWT Secret (change in production!)
jwt.secret=your-256-bit-secret-key-change-this-in-production
jwt.expiration=86400000

# Admin Bootstrap
llmops.admin.email=admin@yourcompany.com
llmops.admin.password=ChangeMe123!
llmops.allowed.domains=yourcompany.com
EOF

# Run Spring Boot
./mvnw spring-boot:run
```

The API will start on `http://localhost:8080`

### 4. FastAPI Evaluation Engine

```bash
cd evaluation-engine

# Create virtual environment
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Configure .env
cp .env.example .env
# Edit .env with your database URL

# Run FastAPI
python run.py
```

The evaluation engine will start on `http://localhost:8000`

### 5. Next.js Frontend

```bash
cd frontend

# Install dependencies
npm install

# Configure environment
cat > .env.local <<EOF
NEXT_PUBLIC_API_URL=http://localhost:8080
EOF

# Run development server
npm run dev
```

The frontend will start on `http://localhost:3000`

### 6. Access the Application

1. Open `http://localhost:3000`
2. Login with admin credentials (from application.properties)
3. Create a project
4. Upload a dataset
5. Configure an LLM endpoint
6. Run your first evaluation!

---

## Docker Compose (Recommended)

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

Access:
- Frontend: `http://localhost:3000`
- Spring Boot API: `http://localhost:8080`
- FastAPI Docs: `http://localhost:8000/docs`

---

## Usage Examples

### 1. Pre-Generated Evaluation (Offline)

You already have LLM outputs and want to evaluate them:

```json
{
  "datasetFormat": "PROMPT_RESPONSE",
  "testCases": [
    {
      "input": "What is the capital of France?",
      "llmOutput": "Paris is the capital of France.",
      "expectedOutput": "Paris",
      "context": null
    }
  ],
  "metrics": ["EXACT_MATCH", "BLEU", "SEMANTIC_SIMILARITY"]
}
```

### 2. Live Generation (Online)

Evaluate your LLM endpoint in real-time:

```json
{
  "datasetFormat": "PROMPT_RESPONSE",
  "testCases": [
    {
      "input": "What is the capital of France?",
      "expectedOutput": "Paris"
    }
  ],
  "targetLLMEndpoint": "your-gpt4-endpoint-id",
  "metrics": ["EXACT_MATCH", "BLEU", "ANSWER_RELEVANCE"]
}
```

### 3. RAG Evaluation

Evaluate retrieval-augmented generation:

```json
{
  "datasetFormat": "RAG_QA",
  "testCases": [
    {
      "input": "What are the company's vacation policies?",
      "context": "Employees receive 15 days of paid vacation...",
      "llmOutput": "The company provides 15 days of vacation.",
      "groundTruth": "15 days paid vacation"
    }
  ],
  "metrics": ["FAITHFULNESS", "CONTEXT_RELEVANCE", "ANSWER_RELEVANCE"]
}
```

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: LLM Evaluation

on:
  pull_request:
    branches: [main]

jobs:
  evaluate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Run LLM Evaluation
        run: |
          curl -X POST https://api.llmops-eval.io/api/v1/evaluations/trigger \
            -H "X-API-Key: ${{ secrets.LLMOPS_API_KEY }}" \
            -H "Content-Type: application/json" \
            -d '{
              "projectId": "${{ secrets.PROJECT_ID }}",
              "datasetId": "${{ secrets.DATASET_ID }}",
              "endpointId": "${{ secrets.ENDPOINT_ID }}",
              "ciMetadata": {
                "gitCommit": "${{ github.sha }}",
                "gitBranch": "${{ github.ref }}",
                "prNumber": "${{ github.event.pull_request.number }}"
              }
            }'

      - name: Check Results
        run: |
          # Add logic to fail PR if scores below threshold
          # See docs/ci-cd-integration.md for details
```

---

## Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| **Backend API** | Spring Boot | 4.0.2 |
| **Language** | Java | 21 |
| **Evaluation Engine** | FastAPI | 0.109+ |
| **Language** | Python | 3.11+ |
| **Frontend** | Next.js | 14 |
| **UI Framework** | React | 18 |
| **Styling** | Tailwind CSS | 3.4 |
| **Database** | PostgreSQL | 16 |
| **Cache** | Redis | 7 |
| **ORM (Java)** | Spring Data JPA | - |
| **ORM (Python)** | SQLAlchemy | 2.0+ |
| **Security** | Spring Security + JWT | 6.x |

### Python Libraries

- **openai** - OpenAI API client
- **anthropic** - Anthropic/Claude API client
- **ragas** - RAG evaluation metrics
- **langchain** - LLM framework
- **sentence-transformers** - Embeddings
- **nltk** - BLEU score
- **rouge-score** - ROUGE metrics

---

## Roadmap

- [x] Spring Boot project setup
- [x] Database schema design
- [x] JWT authentication
- [x] Project & Dataset management
- [x] FastAPI evaluation engine
- [x] Next.js frontend
- [ ] Flyway migrations
- [ ] Invite-only user system
- [ ] RAG metrics (RAGAS integration)
- [ ] LLM-as-Judge framework
- [ ] Scheduled evaluations
- [ ] Advanced reporting & analytics
- [ ] Kubernetes deployment configs

See [docs/LLMEvalDevOps-Platform-Design.md](docs/LLMEvalDevOps-Platform-Design.md) for detailed roadmap.

---

## Documentation

- [Platform Design](docs/LLMEvalDevOps-Platform-Design.md) - Comprehensive architecture and design
- [Evaluation Engine](docs/evaluation-engine.md) - FastAPI engine details
- API Documentation - Visit `/api/docs` (Swagger UI)
- FastAPI Docs - Visit `http://localhost:8000/docs`

---

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

### Development Setup

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests (`./mvnw test` and `pytest`)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

---

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---

## Support

- **Issues**: [GitHub Issues](https://github.com/ashwithpoojary98/llm-eval-ops/issues)
- **Discussions**: [GitHub Discussions](https://github.com/ashwithpoojary98/llm-eval-ops/discussions)
- **Email**: ashwithpoojary98@gmail.com

---

## Acknowledgments

- [RAGAS](https://github.com/explodinggradients/ragas) - RAG evaluation framework
- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- [FastAPI](https://fastapi.tiangolo.com/) - Modern Python web framework
- [Next.js](https://nextjs.org/) - React framework

---

## Star History

If you find this project useful, please consider giving it a star ⭐

---

**Made with ❤️ for the LLM community**

*Making LLM evaluation accessible to everyone*
