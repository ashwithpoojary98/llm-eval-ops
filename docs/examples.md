# LLMOps Eval — Real-World Examples

Five end-to-end scenarios showing exactly how the platform is used, including real API payloads, expected responses, and the business problem each one solves.

---

## Example 1 — Customer Support Bot Quality Gate

**Scenario:** A fintech company has a customer support chatbot that answers billing questions. Before every deployment they run 200 test cases to ensure the bot doesn't regress. If any metric drops below threshold, the deployment is blocked automatically via webhook.

### Step 1 — Start the evaluation (PRE_GENERATED mode)

The LLM output was already captured in staging. You only want to measure quality, not regenerate answers.

```bash
curl -X POST http://localhost:8080/api/evaluations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_TOKEN" \
  -d '{
    "projectId": "proj_fintech_support",
    "name": "Support Bot v2.4.1 — Pre-Deploy Gate",
    "evaluationMode": "PRE_GENERATED",
    "callbackUrl": "https://ci.company.com/webhooks/llmops",
    "metrics": [
      { "code": "ROUGE_L",            "weight": 1.0, "passThreshold": 0.55 },
      { "code": "SEMANTIC_SIMILARITY", "weight": 2.0, "passThreshold": 0.80 },
      { "code": "RELEVANCE",           "weight": 2.0, "passThreshold": 0.75 },
      { "code": "FLUENCY",             "weight": 1.0, "passThreshold": 0.85 },
      { "code": "TOXICITY",            "weight": 3.0, "passThreshold": 0.95 }
    ],
    "judgeEndpointId": "endpoint_gpt4_judge",
    "testCases": [
      {
        "id": "tc_001",
        "question": "Why was I charged twice this month?",
        "llmOutput": "This can happen if a payment failed and was retried. Please check your transaction history under Account > Billing. If both charges appear as successful, contact us and we will issue a refund within 3-5 business days.",
        "groundTruth": "Duplicate charges occur when an initial payment fails and the system retries. Both charges may appear temporarily. Successful duplicates are refunded within 3-5 business days."
      },
      {
        "id": "tc_002",
        "question": "How do I cancel my subscription?",
        "llmOutput": "You can cancel anytime from Settings > Subscription > Cancel Plan. Your access continues until the end of your current billing period. No refunds are issued for partial months.",
        "groundTruth": "Cancel via Settings > Subscription > Cancel Plan. Service continues to the end of the billing cycle. Partial month refunds are not provided."
      }
    ]
  }'
```

**Response:**

```json
{
  "evaluationRunId": "eval_20240315_001",
  "status": "QUEUED",
  "message": "Evaluation queued with 200 test cases",
  "estimatedDurationSeconds": 45
}
```

### Step 2 — Poll for results

```bash
curl http://localhost:8080/api/evaluations/eval_20240315_001/results \
  -H "Authorization: Bearer $API_TOKEN"
```

```json
{
  "evaluationRunId": "eval_20240315_001",
  "status": "COMPLETED",
  "overallScore": 0.847,
  "totalTestCases": 200,
  "completedCount": 198,
  "failedCount": 2,
  "metricSummaries": [
    {
      "metricCode": "ROUGE_L",
      "averageScore": 0.612,
      "minScore": 0.21,
      "maxScore": 0.94,
      "passRate": 0.89
    },
    {
      "metricCode": "SEMANTIC_SIMILARITY",
      "averageScore": 0.831,
      "minScore": 0.54,
      "maxScore": 0.97,
      "passRate": 0.82
    },
    {
      "metricCode": "TOXICITY",
      "averageScore": 0.983,
      "minScore": 0.91,
      "maxScore": 1.00,
      "passRate": 1.00
    }
  ],
  "totalCostUsd": 0.34,
  "completedAt": "2024-03-15T10:32:18Z"
}
```

### Step 3 — Webhook payload sent to CI/CD

When evaluation completes, the platform fires a signed webhook to your CI server:

```json
{
  "event": "EVALUATION_COMPLETED",
  "timestamp": "2024-03-15T10:32:18Z",
  "data": {
    "evaluationRunId": "eval_20240315_001",
    "projectId": "proj_fintech_support",
    "overallScore": 0.847,
    "passed": true,
    "failedMetrics": [],
    "completedCount": 198,
    "failedCount": 2
  }
}
```

Signature header for verification:
```
X-LLMOps-Signature: sha256=a7f3c91b2d8e4f6a0c3b5d7e9f1a3c5b7d9e1f3a5c7b9d1e3f5a7c9b1d3e5f7
```

Your CI script:
```bash
# Block deployment if overall score < 0.80 or any metric failed pass threshold
if [ "$(echo $PAYLOAD | jq '.data.passed')" = "false" ]; then
  echo "Quality gate FAILED — deployment blocked"
  exit 1
fi
```

---

## Example 2 — Medical RAG Chatbot (Faithfulness is Non-Negotiable)

**Scenario:** A healthcare company runs an internal RAG chatbot that answers questions from clinical notes. A hallucinated answer can harm a patient. They run FAITHFULNESS and CONTEXT_RECALL on every build and treat any score below 0.90 as a blocker.

```bash
curl -X POST http://localhost:8080/api/evaluations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_TOKEN" \
  -d '{
    "projectId": "proj_clinical_rag",
    "name": "Clinical RAG — Nightly Faithfulness Gate",
    "evaluationMode": "PRE_GENERATED",
    "metrics": [
      { "code": "FAITHFULNESS",       "weight": 5.0, "passThreshold": 0.90 },
      { "code": "CONTEXT_RECALL",     "weight": 3.0, "passThreshold": 0.85 },
      { "code": "CONTEXT_PRECISION",  "weight": 2.0, "passThreshold": 0.80 },
      { "code": "ANSWER_CORRECTNESS", "weight": 4.0, "passThreshold": 0.88 }
    ],
    "judgeEndpointId": "endpoint_claude3_judge",
    "testCases": [
      {
        "id": "tc_clinical_001",
        "question": "What medication is the patient currently taking for hypertension?",
        "llmOutput": "The patient is currently taking Lisinopril 10mg daily for hypertension management.",
        "groundTruth": "Lisinopril 10mg once daily",
        "retrievedContext": [
          "Current medications: Lisinopril 10mg PO QD, Metformin 500mg PO BID",
          "Diagnosis: Essential hypertension (I10), Type 2 diabetes mellitus (E11.9)",
          "Allergies: Penicillin (rash), Sulfa drugs"
        ]
      },
      {
        "id": "tc_clinical_002",
        "question": "Does this patient have any drug allergies?",
        "llmOutput": "The patient is allergic to Penicillin which causes a rash, and also has an allergy to Sulfa drugs.",
        "groundTruth": "Penicillin (rash), Sulfa drugs",
        "retrievedContext": [
          "Allergies: Penicillin (rash), Sulfa drugs",
          "Last allergy review: 2024-01-10"
        ]
      },
      {
        "id": "tc_clinical_003_hallucination_test",
        "question": "Is the patient taking Aspirin?",
        "llmOutput": "Yes, the patient takes Aspirin 81mg daily for cardiovascular protection.",
        "groundTruth": "No aspirin is mentioned in the patient record.",
        "retrievedContext": [
          "Current medications: Lisinopril 10mg PO QD, Metformin 500mg PO BID",
          "No antiplatelet therapy documented"
        ]
      }
    ]
  }'
```

**What this catches:** test case `tc_clinical_003` has a hallucinated answer — the bot claimed the patient takes Aspirin when the context says otherwise. FAITHFULNESS will score this ~0.05 (near-zero), immediately flagging the model as unsafe for production.

**Result for the hallucination test case:**
```json
{
  "testCaseId": "tc_clinical_003_hallucination_test",
  "status": "COMPLETED",
  "scores": [
    {
      "metricCode": "FAITHFULNESS",
      "score": 0.04,
      "passed": false,
      "reasoning": "The claim 'patient takes Aspirin 81mg daily' is not supported by any provided context. Context explicitly states no antiplatelet therapy is documented."
    },
    {
      "metricCode": "CONTEXT_RECALL",
      "score": 0.12,
      "passed": false
    }
  ]
}
```

---

## Example 3 — Comparing GPT-4o vs Claude 3.5 Sonnet for Code Generation

**Scenario:** A developer tools company is deciding which LLM to use for their code autocomplete feature. They use LIVE_GENERATION mode to generate answers from both models against the same test set, then compare results.

### Run A — GPT-4o

```bash
curl -X POST http://localhost:8080/api/evaluations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_TOKEN" \
  -d '{
    "projectId": "proj_code_assistant",
    "name": "Code Gen — GPT-4o Benchmark",
    "evaluationMode": "LIVE_GENERATION",
    "metrics": [
      { "code": "EXACT_MATCH",       "weight": 3.0, "passThreshold": 0.40 },
      { "code": "BLEU",              "weight": 2.0, "passThreshold": 0.50 },
      { "code": "SEMANTIC_SIMILARITY","weight": 2.0, "passThreshold": 0.80 },
      { "code": "COHERENCE",         "weight": 1.0, "passThreshold": 0.85 },
      { "code": "LATENCY",           "weight": 1.0 },
      { "code": "COST",              "weight": 1.0 }
    ],
    "targetLlmEndpoint": {
      "providerType": "OPENAI",
      "modelName": "gpt-4o",
      "apiKey": "sk-...",
      "config": { "temperature": 0.0, "maxTokens": 512 }
    },
    "judgeEndpointId": "endpoint_gpt4_judge",
    "testCases": [
      {
        "id": "code_001",
        "question": "Write a Python function that checks if a string is a palindrome, ignoring spaces and case.",
        "groundTruth": "def is_palindrome(s):\n    cleaned = s.replace(' ', '').lower()\n    return cleaned == cleaned[::-1]"
      },
      {
        "id": "code_002",
        "question": "Write a SQL query to find the top 5 customers by total order value in the last 30 days.",
        "groundTruth": "SELECT customer_id, SUM(order_value) as total\nFROM orders\nWHERE created_at >= NOW() - INTERVAL '30 days'\nGROUP BY customer_id\nORDER BY total DESC\nLIMIT 5;"
      }
    ]
  }'
```

### Run B — Claude 3.5 Sonnet (same test cases, different target)

```json
{
  "name": "Code Gen — Claude 3.5 Sonnet Benchmark",
  "targetLlmEndpoint": {
    "providerType": "ANTHROPIC",
    "modelName": "claude-3-5-sonnet-20241022",
    "apiKey": "sk-ant-..."
  }
}
```

### Comparison Result

| Metric              | GPT-4o | Claude 3.5 Sonnet |
|---------------------|--------|-------------------|
| Exact Match         | 0.38   | 0.41              |
| BLEU                | 0.64   | 0.71              |
| Semantic Similarity | 0.89   | 0.91              |
| Coherence           | 0.88   | 0.90              |
| Avg Latency (ms)    | 1840   | 1210              |
| Avg Cost / 1K calls | $0.024 | $0.018            |
| **Overall Score**   | **0.812** | **0.851**      |

**Business decision:** Claude 3.5 Sonnet scores higher, is 34% faster, and 25% cheaper — clear winner for this use case.

---

## Example 4 — LLM Health Monitoring + PagerDuty Alert

**Scenario:** A startup's product depends entirely on the OpenAI API. They register a webhook on `LLM_HEALTH_DOWN` to page on-call engineers via PagerDuty the moment the provider goes down.

### Step 1 — Register the endpoint in Admin UI

Via the Admin > Settings panel, add the LLM endpoint:

```json
{
  "name": "OpenAI GPT-4o Production",
  "providerType": "OPENAI",
  "modelName": "gpt-4o",
  "apiKey": "sk-...",
  "isActive": true
}
```

The health scheduler probes this endpoint every 5 minutes automatically.

### Step 2 — Register the webhook

```bash
curl -X POST http://localhost:8080/api/webhooks \
  -H "Authorization: Bearer $API_TOKEN" \
  -d '{
    "name": "PagerDuty LLM Health Alerts",
    "url": "https://events.pagerduty.com/v2/enqueue",
    "secret": "your-webhook-secret-for-hmac",
    "events": ["LLM_HEALTH_DOWN", "LLM_HEALTH_RECOVERED", "LLM_HEALTH_DEGRADED"]
  }'
```

### Step 3 — Webhook fires when OpenAI goes down

```json
{
  "event": "LLM_HEALTH_DOWN",
  "timestamp": "2024-03-15T14:22:01Z",
  "signature": "sha256=b3f8a1c2d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1",
  "data": {
    "endpointId": "endpoint_openai_gpt4o_prod",
    "endpointName": "OpenAI GPT-4o Production",
    "providerType": "OPENAI",
    "modelName": "gpt-4o",
    "previousStatus": "UP",
    "currentStatus": "DOWN",
    "consecutiveFailures": 2,
    "lastLatencyMs": null,
    "errorMessage": "connect timeout after 10000ms",
    "uptimeLast24h": 0.9986
  }
}
```

### Step 4 — Webhook fires again when it recovers

```json
{
  "event": "LLM_HEALTH_RECOVERED",
  "data": {
    "endpointName": "OpenAI GPT-4o Production",
    "previousStatus": "DOWN",
    "currentStatus": "UP",
    "downtimeDurationMinutes": 18,
    "lastLatencyMs": 1243,
    "uptimeLast24h": 0.9874
  }
}
```

Your PagerDuty bridge script (or a simple Lambda/Cloud Function):

```python
import hmac, hashlib, json
from flask import Flask, request

app = Flask(__name__)
WEBHOOK_SECRET = "your-webhook-secret-for-hmac"

@app.route("/llmops-webhook", methods=["POST"])
def handle():
    # Verify HMAC signature
    sig = request.headers.get("X-LLMOps-Signature", "")
    body = request.get_data()
    expected = "sha256=" + hmac.new(
        WEBHOOK_SECRET.encode(), body, hashlib.sha256
    ).hexdigest()
    if not hmac.compare_digest(sig, expected):
        return "Unauthorized", 401

    payload = json.loads(body)
    event = payload["event"]

    if event == "LLM_HEALTH_DOWN":
        pagerduty_trigger(
            summary=f"LLM DOWN: {payload['data']['endpointName']}",
            severity="critical",
            details=payload["data"],
        )
    elif event == "LLM_HEALTH_RECOVERED":
        pagerduty_resolve(
            summary=f"LLM RECOVERED: {payload['data']['endpointName']}",
        )

    return "OK", 200
```

---

## Example 5 — Monthly Model Drift Report

**Scenario:** An e-commerce company runs a weekly automated evaluation to detect if their fine-tuned recommendation LLM is drifting over time as the product catalog changes. They track scores week-over-week via the API and send a Slack summary.

### The weekly evaluation script

```python
import httpx
import time
from datetime import datetime

LLMOPS_URL = "http://llmops.internal"
API_TOKEN  = "Bearer eyJ..."

# 50 golden test cases stored in your own database
test_cases = load_golden_test_cases()  # returns list of dicts

# Build evaluation request
payload = {
    "projectId": "proj_ecommerce_recs",
    "name": f"Weekly Drift Check — {datetime.now().strftime('%Y-W%U')}",
    "evaluationMode": "LIVE_GENERATION",
    "targetLlmEndpoint": {
        "providerType": "CUSTOM",
        "modelName": "rec-llm-v3-finetuned",
        "apiKey": "internal-key",
        "apiUrl": "http://rec-llm.internal/v1"
    },
    "judgeEndpointId": "endpoint_gpt4_judge",
    "metrics": [
        {"code": "SEMANTIC_SIMILARITY", "weight": 3.0, "passThreshold": 0.78},
        {"code": "RELEVANCE",           "weight": 3.0, "passThreshold": 0.75},
        {"code": "ANSWER_CORRECTNESS",  "weight": 2.0, "passThreshold": 0.80},
        {"code": "BERTSCORE",           "weight": 2.0, "passThreshold": 0.72},
    ],
    "testCases": test_cases,
    "callbackUrl": "http://llmops.internal/api/internal/weekly-callback",
}

with httpx.Client() as client:
    # Start evaluation
    resp = client.post(
        f"{LLMOPS_URL}/api/evaluations",
        json=payload,
        headers={"Authorization": API_TOKEN},
    )
    run_id = resp.json()["evaluationRunId"]
    print(f"Started: {run_id}")

    # Poll until complete
    while True:
        status_resp = client.get(
            f"{LLMOPS_URL}/api/evaluations/{run_id}/status",
            headers={"Authorization": API_TOKEN},
        )
        status = status_resp.json()["status"]
        if status in ("COMPLETED", "FAILED"):
            break
        time.sleep(10)

    # Fetch results
    results = client.get(
        f"{LLMOPS_URL}/api/evaluations/{run_id}/results",
        headers={"Authorization": API_TOKEN},
    ).json()

# Compare with last week
last_week_score = load_last_week_score("proj_ecommerce_recs")
this_week_score = results["overallScore"]
delta = this_week_score - last_week_score

# Post to Slack
slack_message = {
    "blocks": [
        {
            "type": "header",
            "text": {"type": "plain_text", "text": "Weekly LLM Drift Report"}
        },
        {
            "type": "section",
            "fields": [
                {"type": "mrkdwn", "text": f"*Overall Score:* {this_week_score:.3f}"},
                {"type": "mrkdwn", "text": f"*Week-over-Week:* {'↓' if delta < 0 else '↑'} {delta:+.3f}"},
                {"type": "mrkdwn", "text": f"*Semantic Similarity:* {results['metricSummaries'][0]['averageScore']:.3f}"},
                {"type": "mrkdwn", "text": f"*Answer Correctness:* {results['metricSummaries'][2]['averageScore']:.3f}"},
            ]
        },
        {
            "type": "section",
            "text": {
                "type": "mrkdwn",
                "text": f"{'🚨 *DRIFT DETECTED* — model performance declined >5%. Review recommended.' if delta < -0.05 else '✅ Model performance is stable.'}"
            }
        }
    ]
}

# Send to Slack
httpx.post(SLACK_WEBHOOK_URL, json=slack_message)
```

### Sample Slack output

```
Weekly LLM Drift Report
─────────────────────────────────
Overall Score:       0.791
Week-over-Week:      ↓ -0.068
Semantic Similarity: 0.763
Answer Correctness:  0.798

🚨 DRIFT DETECTED — model performance declined >5%. Review recommended.
```

---

## Example 6 — Admin: Configure SMTP and Send Evaluation Report by Email

**Scenario:** An enterprise customer wants evaluation completion reports sent to their team by email. An admin configures the SMTP server through the UI (no server restart needed).

### Configure SMTP via Admin Settings API

```bash
curl -X PUT http://localhost:8080/api/admin/settings/email \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "smtpHost": "smtp.company.com",
    "smtpPort": 587,
    "username": "llmops-alerts@company.com",
    "password": "smtp-app-password",
    "fromAddress": "llmops-alerts@company.com",
    "fromName": "LLMOps Eval Platform",
    "enableTls": true,
    "enableSsl": false
  }'
```

### Send a test email to confirm it works

```bash
curl -X POST http://localhost:8080/api/admin/settings/email/test \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{ "recipient": "mlteam@company.com" }'
```

```json
{
  "success": true,
  "latencyMs": 342,
  "message": "Test email delivered successfully"
}
```

Now when an evaluation finishes, the Spring Boot backend automatically emails the team using the organization's own SMTP server — no shared credentials, no vendor dependency.

---

## Putting It All Together — A Full DevOps Pipeline

```
Developer pushes code
        │
        ▼
  GitHub Actions CI
        │
        ├─► Run unit tests
        │
        ├─► Deploy to staging
        │
        └─► POST /api/evaluations  ◄── 200 test cases from golden dataset
                    │
                    ▼
          LLMOps Eval Platform
          ┌─────────────────────────────────────┐
          │  Evaluation Engine runs in parallel  │
          │  25 metrics · HMAC-signed callback  │
          └──────────────────┬──────────────────┘
                             │
               ┌─────────────▼─────────────┐
               │                           │
          overallScore ≥ 0.80         overallScore < 0.80
               │                           │
               ▼                           ▼
        ✅ Deploy to prod          🚨 Block deployment
        Send Slack: "v2.5 live"   Page on-call engineer
                                  Open GitHub issue with
                                  failing test case IDs
```

This pipeline means **bad models never reach production** and your team is automatically notified — without any manual review step.
