---
sidebar_position: 6
title: CI/CD Integration
---

# CI/CD Integration

Integrate LLMOps Eval into your CI/CD pipeline to enforce quality gates on every deployment.

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

      - name: Trigger LLM Evaluation
        run: |
          curl -X POST ${{ secrets.LLMOPS_API_URL }}/api/v1/evaluations/trigger \
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
```

## GitLab CI

```yaml
llm-evaluation:
  stage: test
  script:
    - |
      curl -X POST $LLMOPS_API_URL/api/v1/evaluations/trigger \
        -H "X-API-Key: $LLMOPS_API_KEY" \
        -H "Content-Type: application/json" \
        -d "{
          \"projectId\": \"$PROJECT_ID\",
          \"datasetId\": \"$DATASET_ID\",
          \"endpointId\": \"$ENDPOINT_ID\"
        }"
```

## Required Secrets

| Secret | Description |
|---|---|
| `LLMOPS_API_URL` | URL of your deployed LLMOps Eval instance |
| `LLMOPS_API_KEY` | API key from Settings → API Keys |
| `PROJECT_ID` | Your project UUID |
| `DATASET_ID` | Dataset UUID to evaluate against |
| `ENDPOINT_ID` | LLM endpoint UUID to evaluate |

## Quality Gates

Configure pass/fail thresholds in your project settings. Evaluations that fall below the threshold will return a non-zero exit code, failing the CI pipeline.

Example threshold configuration:

```json
{
  "thresholds": {
    "faithfulness": 0.80,
    "answer_relevancy": 0.75,
    "overall_score": 0.78
  }
}
```
