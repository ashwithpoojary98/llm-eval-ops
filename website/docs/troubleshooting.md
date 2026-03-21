---
sidebar_position: 7
title: Troubleshooting
---

# Troubleshooting

Common issues and how to resolve them.

---

## Backend won't start

### `Failed to load ApplicationContext` / SMTP error

Spring Boot tries to connect to your mail server at startup. If you have no SMTP configured:

```yaml
# application.yaml or environment variable
spring:
  mail:
    host: ""  # leave empty to disable mail
```

Or set the environment variable:
```bash
SPRING_MAIL_HOST=""
```

### `JWT_SECRET_KEY is too short`

The JWT secret must be at least 32 characters. Generate a secure value:

```bash
openssl rand -hex 32
```

### `APP_ENCRYPTION_KEY must be exactly 32 characters`

The encryption key for LLM API key storage must be exactly 32 characters:

```bash
openssl rand -hex 16  # generates exactly 32 hex chars
```

---

## Evaluation engine won't start

### `NLTK resource not found`

The NLTK data is downloaded during Docker build. For local dev:

```bash
python -c "import nltk; nltk.download('punkt'); nltk.download('punkt_tab'); nltk.download('wordnet')"
```

### `Connection refused` to PostgreSQL

Make sure the `DATABASE_URL` in `evaluation-engine/.env` points to the correct host:

```env
# Docker Compose — use service name
DATABASE_URL=postgresql+asyncpg://postgres:postgres@postgres:5432/llmevalplatform

# Local dev — use localhost
DATABASE_URL=postgresql+asyncpg://postgres:postgres@localhost:5432/llmevalplatform
```

---

## Evaluation stays in PENDING state

This usually means the evaluation engine can't reach the Spring Boot callback URL, or the callback secret doesn't match.

1. Check the evaluation engine logs: `docker logs llmops-eval-engine`
2. Verify `EVALUATION_ENGINE_URL` in the backend points to the engine
3. Verify `EVALUATION_CALLBACK_SECRET` / `CALLBACK_SECRET` match in both services
4. Check the engine can reach the backend: `curl http://backend:8080/api/auth/health`

---

## Frontend can't reach the API

### `CORS error` in browser console

Set the allowed origins on the backend:

```bash
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
```

Multiple origins (comma-separated):

```bash
CORS_ALLOWED_ORIGINS=https://app.example.com,https://staging.example.com
```

### `NEXT_PUBLIC_API_URL` is wrong

This must be set at **build time** for Next.js (it's baked into the JS bundle):

```bash
# Docker build
docker build --build-arg NEXT_PUBLIC_API_URL=https://api.example.com/api .

# Local dev
echo "NEXT_PUBLIC_API_URL=http://localhost:8080/api" > frontend/.env.local
```

---

## Docker Compose issues

### Port already in use

```bash
# Find what's using port 8080
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Or change the port in docker-compose.yml
ports:
  - "8081:8080"  # host:container
```

### Container exits immediately

Check logs before it exits:
```bash
docker-compose logs backend
docker-compose logs evaluation-engine
docker-compose logs frontend
```

---

## Getting help

- **GitHub Issues**: [github.com/ashwithpoojary98/llm-eval-ops/issues](https://github.com/ashwithpoojary98/llm-eval-ops/issues)
- **API reference**: Available at `http://localhost:8080/swagger-ui.html` when running locally
- **Eval engine API docs**: Available at `http://localhost:8000/docs`
