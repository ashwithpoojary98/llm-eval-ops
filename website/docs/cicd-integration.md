---
sidebar_position: 6
title: CI/CD Integration
---

# CI/CD Integration

Integrate LLMOps Eval into your CI/CD pipeline to run evaluations automatically on every deployment.

## Authentication

LLMOps Eval uses JWT authentication. Obtain a token by calling the login endpoint:

```bash
TOKEN=$(curl -s -X POST https://your-llmops-instance/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ci@example.com","password":"${{ secrets.LLMOPS_PASSWORD }}"}' \
  | jq -r '.accessToken')
```

> **Tip:** Create a dedicated CI service account with limited permissions (Member role) to scope access.

---

## GitHub Actions

```yaml
name: LLM Evaluation

on:
  pull_request:
    branches: [main]

jobs:
  evaluate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Authenticate with LLMOps Eval
        id: auth
        run: |
          TOKEN=$(curl -s -X POST ${{ secrets.LLMOPS_API_URL }}/api/auth/login \
            -H "Content-Type: application/json" \
            -d '{"email":"${{ secrets.LLMOPS_CI_EMAIL }}","password":"${{ secrets.LLMOPS_CI_PASSWORD }}"}' \
            | jq -r '.accessToken')
          echo "token=$TOKEN" >> $GITHUB_OUTPUT

      - name: Trigger LLM Evaluation
        id: trigger
        run: |
          RESPONSE=$(curl -s -X POST \
            ${{ secrets.LLMOPS_API_URL }}/api/projects/${{ secrets.PROJECT_ID }}/evaluations \
            -H "Authorization: Bearer ${{ steps.auth.outputs.token }}" \
            -H "Content-Type: application/json" \
            -d '{
              "datasetId": "${{ secrets.DATASET_ID }}",
              "endpointId": "${{ secrets.ENDPOINT_ID }}",
              "metrics": ["faithfulness", "answer_relevancy", "bleu"]
            }')
          echo "evaluation_id=$(echo $RESPONSE | jq -r '.id')" >> $GITHUB_OUTPUT

      - name: Wait for Results
        run: |
          for i in $(seq 1 30); do
            STATUS=$(curl -s \
              ${{ secrets.LLMOPS_API_URL }}/api/evaluations/${{ steps.trigger.outputs.evaluation_id }} \
              -H "Authorization: Bearer ${{ steps.auth.outputs.token }}" \
              | jq -r '.status')
            echo "Status: $STATUS"
            if [ "$STATUS" = "COMPLETED" ]; then break; fi
            if [ "$STATUS" = "FAILED" ]; then exit 1; fi
            sleep 10
          done
```

---

## GitLab CI

```yaml
llm-evaluation:
  stage: test
  script:
    - |
      TOKEN=$(curl -s -X POST $LLMOPS_API_URL/api/auth/login \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$LLMOPS_CI_EMAIL\",\"password\":\"$LLMOPS_CI_PASSWORD\"}" \
        | jq -r '.accessToken')

      curl -s -X POST $LLMOPS_API_URL/api/projects/$PROJECT_ID/evaluations \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
          \"datasetId\": \"$DATASET_ID\",
          \"endpointId\": \"$ENDPOINT_ID\",
          \"metrics\": [\"faithfulness\", \"answer_relevancy\", \"bleu\"]
        }"
```

---

## Required Secrets

| Secret | Description |
|---|---|
| `LLMOPS_API_URL` | URL of your deployed LLMOps Eval instance (e.g. `https://llmops.example.com`) |
| `LLMOPS_CI_EMAIL` | Email of a dedicated CI service account |
| `LLMOPS_CI_PASSWORD` | Password for the CI service account |
| `PROJECT_ID` | Your project UUID (visible in the project settings URL) |
| `DATASET_ID` | Dataset UUID to evaluate against |
| `ENDPOINT_ID` | LLM endpoint UUID to evaluate |

---

## Available Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/projects/{projectId}/evaluations` | Trigger a new evaluation run |
| `GET` | `/api/projects/{projectId}/evaluations` | List all runs for a project |
| `GET` | `/api/evaluations/{evaluationId}` | Get evaluation status and summary |
| `GET` | `/api/evaluations/{evaluationId}/results` | Get detailed metric results |
| `POST` | `/api/evaluations/{evaluationId}/cancel` | Cancel a running evaluation |
| `POST` | `/api/evaluations/{evaluationId}/retrigger` | Re-run a previous evaluation |
