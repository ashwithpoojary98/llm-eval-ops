# Testing Guide — Verify Everything Works

Step-by-step from zero to a working evaluation. Each step has a success check so you know exactly when something is broken.

---

## Step 1 — Start the Platform

```bash
cd llmops-eval
cp .env.example .env

# Minimum required in .env:
# DB_PASSWORD=postgres
# JWT_SECRET=any-long-random-string-here
# OPENAI_API_KEY=sk-...   (only needed for LIVE_GENERATION or LLM-judge metrics)

docker compose up -d
```

**Success check:**
```bash
docker compose ps
```
All 4 services should show `healthy`:
```
NAME               STATUS
llmops-postgres    Up (healthy)
llmops-backend     Up (healthy)
llmops-eval-engine Up (healthy)
llmops-frontend    Up (healthy)
```

---

## Step 2 — Verify Each Service Health Endpoint

```bash
# Backend (Spring Boot)
curl http://localhost:8080/api/actuator/health
# Expected: {"status":"UP"}

# Evaluation Engine (FastAPI)
curl http://localhost:8000/health
# Expected: {"status":"healthy","database":"connected","version":"0.1.0"}

# Evaluation Engine readiness (Kubernetes probe — was broken, now fixed)
curl http://localhost:8000/health/ready
# Expected: {"status":"ready"}

# Frontend
curl -I http://localhost:3000
# Expected: HTTP/1.1 200 OK
```

---

## Step 3 — Register a User and Get a Token

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test1234!",
    "firstName": "Test",
    "lastName": "User",
    "organizationName": "Test Org"
  }'

# Login and grab the token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234!"}' \
  | jq -r '.token')

echo "Token: $TOKEN"
```

**Success check:** Token is a non-empty JWT string starting with `eyJ`.

---

## Step 4 — Create a Project

```bash
# First create or find your organization ID
ORG_ID=$(curl -s http://localhost:8080/api/organizations \
  -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')

# Create project
PROJECT_ID=$(curl -s -X POST http://localhost:8080/api/projects \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"Test Project\",
    \"description\": \"Smoke test project\",
    \"organizationId\": \"$ORG_ID\"
  }" | jq -r '.id')

echo "Project ID: $PROJECT_ID"
```

---

## Step 5 — Run a Smoke Test Evaluation (No LLM API Key Needed)

This uses only free, local metrics (ROUGE, BLEU, Exact Match) — no external API calls.

```bash
curl -X POST http://localhost:8080/api/evaluations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"projectId\": \"$PROJECT_ID\",
    \"name\": \"Smoke Test\",
    \"evaluationMode\": \"PRE_GENERATED\",
    \"metrics\": [
      {\"code\": \"BLEU\",        \"weight\": 1.0, \"passThreshold\": 0.3},
      {\"code\": \"ROUGE_L\",     \"weight\": 1.0, \"passThreshold\": 0.4},
      {\"code\": \"EXACT_MATCH\", \"weight\": 1.0}
    ],
    \"testCases\": [
      {
        \"id\": \"tc_1\",
        \"question\": \"What is the capital of France?\",
        \"llmOutput\": \"The capital of France is Paris.\",
        \"groundTruth\": \"Paris is the capital of France.\"
      },
      {
        \"id\": \"tc_2\",
        \"question\": \"What is 2 + 2?\",
        \"llmOutput\": \"4\",
        \"groundTruth\": \"4\"
      },
      {
        \"id\": \"tc_3\",
        \"question\": \"What colour is the sky?\",
        \"llmOutput\": \"The sky is blue during a clear day.\",
        \"groundTruth\": \"Blue\"
      }
    ]
  }"
```

**Expected response:**
```json
{
  "evaluationRunId": "some-uuid",
  "status": "QUEUED",
  "message": "Evaluation queued with 3 test cases"
}
```

Save the run ID:
```bash
RUN_ID="<paste evaluationRunId here>"
```

---

## Step 6 — Check Evaluation Status

```bash
curl http://localhost:8080/api/evaluations/$RUN_ID/status \
  -H "Authorization: Bearer $TOKEN"
```

Poll until `status` is `COMPLETED` (takes 5-15 seconds for 3 test cases):

```json
{
  "evaluationRunId": "...",
  "status": "COMPLETED",
  "progress": { "completed": 3, "total": 3, "failed": 0 }
}
```

If status stays `PENDING` forever → UUID bug was not fixed. Check evaluation-engine logs:
```bash
docker compose logs eval-engine --tail=50
```

---

## Step 7 — Fetch Full Results

```bash
curl "http://localhost:8080/api/evaluations/$RUN_ID/results" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

**Expected structure:**
```json
{
  "evaluationRunId": "...",
  "status": "COMPLETED",
  "overallScore": 0.72,
  "totalTestCases": 3,
  "completedCount": 3,
  "failedCount": 0,
  "metricSummaries": [
    { "metricCode": "BLEU",        "averageScore": 0.61, "passRate": 1.0 },
    { "metricCode": "ROUGE_L",     "averageScore": 0.73, "passRate": 1.0 },
    { "metricCode": "EXACT_MATCH", "averageScore": 0.33, "passRate": null }
  ],
  "results": [
    {
      "testCaseId": "tc_2",
      "scores": [
        { "metricCode": "EXACT_MATCH", "score": 1.0, "passed": null }
      ]
    }
  ]
}
```

**tc_2** should have `EXACT_MATCH: 1.0` because `"4" == "4"`. This confirms the full pipeline works end-to-end.

---

## Step 8 — Test the Webhook System

Start a listener (uses [webhook.site](https://webhook.site) or run locally with netcat):

```bash
# Quick local listener on port 9999
python3 -m http.server 9999 &
LISTENER_PID=$!
```

Or use the built-in test button in Admin > Webhooks UI.

Register a webhook via API:
```bash
curl -X POST http://localhost:8080/api/webhooks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"organizationId\": \"$ORG_ID\",
    \"name\": \"Test Webhook\",
    \"url\": \"http://host.docker.internal:9999\",
    \"secret\": \"test-secret-123\",
    \"events\": [\"EVALUATION_COMPLETED\", \"EVALUATION_FAILED\"]
  }"
```

Run a small evaluation — when it completes you should see the POST hit your listener with the signed payload.

Kill the listener: `kill $LISTENER_PID`

---

## Step 9 — Test LLM Health Monitor

```bash
# Add an LLM endpoint (replace with your real key / endpoint)
curl -X POST http://localhost:8080/api/llm-endpoints \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"projectId\": \"$PROJECT_ID\",
    \"name\": \"OpenAI GPT-4o-mini\",
    \"providerType\": \"OPENAI\",
    \"modelName\": \"gpt-4o-mini\",
    \"apiKey\": \"$OPENAI_API_KEY\",
    \"isActive\": true
  }"

# Manually trigger a health check (don't wait 5 minutes)
curl -X POST http://localhost:8080/api/health/check \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:** HTTP 200, and the endpoint now shows UP status in Admin > Health.

---

## Step 10 — Open the UI and Verify Visually

| Page                             | URL                                    | What to verify                     |
|----------------------------------|----------------------------------------|------------------------------------|
| Dashboard                        | http://localhost:3000                  | Shows the project + evaluation run |
| Evaluation Results               | http://localhost:3000/evaluations      | Smoke test shows COMPLETED + scores |
| Admin → Health                   | http://localhost:3000/admin/health     | LLM endpoint shows UP + latency    |
| Admin → Webhooks                 | http://localhost:3000/admin/webhooks   | Webhook registered                 |
| Admin → Settings                 | http://localhost:3000/admin/settings   | Email + Platform tabs visible      |

---

## Troubleshooting

### Evaluation stays PENDING forever
```bash
docker compose logs eval-engine --tail=100
```
Look for `ValueError: badly formed hexadecimal UUID string` → UUID bug in `evaluation_service.py`.

### Evaluation Engine can't connect to DB
```bash
docker compose logs eval-engine | grep "database"
```
Check `DATABASE_URL` in `.env`. Should be:
```
DATABASE_URL=postgresql+asyncpg://postgres:postgres@postgres:5432/llmops_eval
```
Note: hostname is `postgres` (the Docker service name), not `localhost`.

### Backend can't start (Flyway migration fails)
```bash
docker compose logs backend | grep "FlywayException"
```
Usually means a migration file has a syntax error. Check `src/main/resources/db/migration/`.

### Frontend shows blank page
```bash
docker compose logs frontend
```
Check `NEXT_PUBLIC_API_URL` is set to `http://localhost:8080/api` (not `https`).

### LLM-as-Judge metrics return score 0.5 for everything
The judge LLM endpoint is not configured or the API key is wrong. Judge metrics default to 0.5 on error (graceful degradation). Check:
```bash
docker compose logs eval-engine | grep "judge"
```

---

## Quick End-to-End Test Script

Save as `test-platform.sh` and run after `docker compose up -d`:

```bash
#!/bin/bash
set -e

BASE="http://localhost:8080/api"
ENGINE="http://localhost:8000"

echo "1. Health checks..."
curl -sf $ENGINE/health | jq -r '"Engine: " + .status'
curl -sf $ENGINE/health/ready | jq -r '"Engine ready: " + .status'
curl -sf $BASE/actuator/health | jq -r '"Backend: " + .status'

echo "2. Auth..."
TOKEN=$(curl -sf -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234!"}' | jq -r '.token')
[ -z "$TOKEN" ] && echo "Login failed — run Step 3 first" && exit 1
echo "Token OK"

echo "3. Running smoke evaluation..."
ORG_ID=$(curl -sf $BASE/organizations -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')
PROJECT_ID=$(curl -sf $BASE/projects -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')

RUN_ID=$(curl -sf -X POST $BASE/evaluations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"projectId\":\"$PROJECT_ID\",\"name\":\"Auto Test\",\"evaluationMode\":\"PRE_GENERATED\",
       \"metrics\":[{\"code\":\"EXACT_MATCH\",\"weight\":1.0}],
       \"testCases\":[{\"id\":\"t1\",\"question\":\"2+2?\",\"llmOutput\":\"4\",\"groundTruth\":\"4\"}]
      }" | jq -r '.evaluationRunId')
echo "Run ID: $RUN_ID"

echo "4. Waiting for completion..."
for i in {1..30}; do
  STATUS=$(curl -sf $BASE/evaluations/$RUN_ID/status \
    -H "Authorization: Bearer $TOKEN" | jq -r '.status')
  [ "$STATUS" = "COMPLETED" ] && break
  [ "$STATUS" = "FAILED" ] && echo "EVALUATION FAILED" && exit 1
  sleep 2
done

echo "5. Checking results..."
SCORE=$(curl -sf $BASE/evaluations/$RUN_ID/results \
  -H "Authorization: Bearer $TOKEN" | jq -r '.results[0].scores[0].score')
[ "$SCORE" = "1.0" ] && echo "✅ ALL TESTS PASSED — Platform is working correctly" \
                      || echo "❌ EXACT_MATCH score should be 1.0, got: $SCORE"
```

```bash
chmod +x test-platform.sh
./test-platform.sh
```
