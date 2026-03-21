"""
evaluate_rag.py — End-to-end RAG evaluation using LLMOps Eval platform.

What this does:
  1. Spins up a simple RAG pipeline against sample_docs/company_faq.txt
  2. Runs 12 test questions through the pipeline (live generation)
  3. Sends results to LLMOps Eval for scoring
  4. Prints a full report table

Usage:
  # With OpenAI (real answers):
  OPENAI_API_KEY=sk-... LLMOPS_TOKEN=eyJ... python evaluate_rag.py

  # Without OpenAI (mock answers, free, still tests the eval pipeline):
  LLMOPS_TOKEN=eyJ... python evaluate_rag.py --mock
"""
import os
import sys
import time
import uuid
import json
import argparse
from datetime import datetime

import httpx
from dotenv import load_dotenv

load_dotenv()

# ── Config ────────────────────────────────────────────────────────────────────

LLMOPS_API     = os.getenv("LLMOPS_API_URL", "http://localhost:8080/api")
LLMOPS_TOKEN   = os.getenv("LLMOPS_TOKEN", "")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "mock")
PROJECT_ID     = os.getenv("LLMOPS_PROJECT_ID", "")

# ── Golden test cases ─────────────────────────────────────────────────────────
# question + ground_truth pairs from the FAQ.
# Ground truth is the canonical answer we want the RAG bot to produce.

GOLDEN_TEST_CASES = [
    {
        "id": "rag_001",
        "question": "How do I cancel my subscription?",
        "ground_truth": "Go to Settings > Subscription > Cancel Plan. Your access continues until the end of your current billing cycle.",
    },
    {
        "id": "rag_002",
        "question": "Why was I charged twice this month?",
        "ground_truth": "Duplicate charges happen when an initial payment attempt fails and the system retries. Contact support for a refund within 3-5 business days.",
    },
    {
        "id": "rag_003",
        "question": "How do I reset my password?",
        "ground_truth": "Click 'Forgot Password' on the login page. You will receive a reset link within 5 minutes. Links expire after 1 hour.",
    },
    {
        "id": "rag_004",
        "question": "What is the API rate limit for the Pro plan?",
        "ground_truth": "Pro plan gets 10,000 API requests per hour. Rate limits reset every hour.",
    },
    {
        "id": "rag_005",
        "question": "Is there a free tier available?",
        "ground_truth": "Yes. The Free tier includes 100 API calls/hour, 1 project, 5 GB storage, and community support. It never expires.",
    },
    {
        "id": "rag_006",
        "question": "How do I contact support?",
        "ground_truth": "Pro plan users can email support@techcorp.com with a 24-hour response SLA. Enterprise gets a dedicated Slack channel.",
    },
    {
        "id": "rag_007",
        "question": "Where is my data stored?",
        "ground_truth": "Data is stored in AWS US-East-1 by default. European customers can opt into EU-West-1 (Ireland).",
    },
    {
        "id": "rag_008",
        "question": "Do you offer discounts for nonprofits?",
        "ground_truth": "Yes, 40% discount for verified nonprofits and 30% for educational institutions.",
    },
    {
        "id": "rag_009",
        "question": "What happens if my payment fails?",
        "ground_truth": "Payment is retried 3 times over 7 days. Account enters a grace period, then is suspended but not deleted if payment still fails.",
    },
    {
        "id": "rag_010",
        "question": "Can I have multiple accounts?",
        "ground_truth": "One account per person. Teams should use the Team plan. Contact sales@techcorp.com.",
    },
    {
        "id": "rag_011",
        "question": "Is the company SOC 2 compliant?",
        "ground_truth": "Yes, SOC 2 Type II certified. Audit report available to Enterprise customers under NDA.",
    },
    # Hallucination test — this information is NOT in the docs
    {
        "id": "rag_012_hallucination",
        "question": "Do you offer a mobile app?",
        "ground_truth": "This information is not available in the provided documentation.",
    },
]

# ── Helpers ───────────────────────────────────────────────────────────────────

def auth_headers() -> dict:
    return {"Authorization": f"Bearer {LLMOPS_TOKEN}", "Content-Type": "application/json"}


def get_or_create_project(client: httpx.Client) -> str:
    """Return existing project ID or create one."""
    if PROJECT_ID:
        return PROJECT_ID

    # List orgs
    orgs = client.get(f"{LLMOPS_API}/organizations", headers=auth_headers()).json()
    if not orgs:
        raise SystemExit("No organizations found. Register a user first.")
    org_id = orgs[0]["id"]

    # List projects
    projects = client.get(
        f"{LLMOPS_API}/projects?organizationId={org_id}", headers=auth_headers()
    ).json()

    # Reuse or create
    rag_project = next((p for p in projects if "RAG Demo" in p.get("name", "")), None)
    if rag_project:
        return rag_project["id"]

    resp = client.post(
        f"{LLMOPS_API}/projects",
        headers=auth_headers(),
        json={
            "name": "RAG Demo — TechCorp FAQ Bot",
            "description": "Evaluating a RAG chatbot over company FAQ documents",
            "organizationId": org_id,
        },
    )
    return resp.json()["id"]


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="RAG Evaluation Demo")
    parser.add_argument("--mock", action="store_true",
                        help="Use mock answers (no OpenAI key needed)")
    parser.add_argument("--metrics", nargs="+",
                        default=["ROUGE_L", "SEMANTIC_SIMILARITY", "FAITHFULNESS",
                                 "CONTEXT_RECALL", "RELEVANCE", "ANSWER_CORRECTNESS"],
                        help="Metrics to evaluate")
    args = parser.parse_args()

    api_key = "mock" if args.mock else OPENAI_API_KEY
    if not LLMOPS_TOKEN:
        print("ERROR: Set LLMOPS_TOKEN env var (get it from the login API or UI)")
        sys.exit(1)

    print("=" * 60)
    print("  RAG Evaluation Demo — LLMOps Eval Platform")
    print("=" * 60)
    print(f"Mode: {'MOCK (free)' if args.mock else 'OPENAI ' + api_key[:8] + '...'}")
    print(f"Test cases: {len(GOLDEN_TEST_CASES)}")
    print(f"Metrics: {', '.join(args.metrics)}")
    print()

    # 1. Build RAG pipeline
    from rag_pipeline import RAGPipeline
    rag = RAGPipeline(openai_api_key=api_key)

    # 2. Run test cases through the pipeline
    print("Running test cases through RAG pipeline...")
    test_case_payloads = []

    for tc in GOLDEN_TEST_CASES:
        result = rag.query(tc["question"])
        print(f"  [{tc['id']}] Q: {tc['question'][:55]}...")
        print(f"         A: {result['answer'][:80]}...")
        print()

        test_case_payloads.append({
            "id": tc["id"],
            "question": tc["question"],
            "llmOutput": result["answer"],
            "groundTruth": tc["ground_truth"],
            "retrievedContext": result["retrieved_context"],
            "metadata": {
                "latency_ms": result["latency_ms"],
                "tokens": result["tokens"],
                "sources": result["top_sources"],
            },
        })

    # 3. Build metric configs
    metric_weight_map = {
        "ROUGE_L":             {"weight": 1.0, "passThreshold": 0.35},
        "BLEU":                {"weight": 1.0, "passThreshold": 0.25},
        "SEMANTIC_SIMILARITY": {"weight": 2.0, "passThreshold": 0.75},
        "BERTSCORE":           {"weight": 2.0, "passThreshold": 0.70},
        "FAITHFULNESS":        {"weight": 4.0, "passThreshold": 0.80},
        "CONTEXT_RECALL":      {"weight": 3.0, "passThreshold": 0.75},
        "CONTEXT_PRECISION":   {"weight": 2.0, "passThreshold": 0.70},
        "ANSWER_RELEVANCY":    {"weight": 2.0, "passThreshold": 0.75},
        "RELEVANCE":           {"weight": 3.0, "passThreshold": 0.78},
        "ANSWER_CORRECTNESS":  {"weight": 4.0, "passThreshold": 0.75},
        "FLUENCY":             {"weight": 1.0, "passThreshold": 0.85},
    }

    metrics_payload = []
    for code in args.metrics:
        cfg = metric_weight_map.get(code, {"weight": 1.0})
        entry = {"code": code, "weight": cfg["weight"]}
        if "passThreshold" in cfg:
            entry["passThreshold"] = cfg["passThreshold"]
        metrics_payload.append(entry)

    # 4. Submit to LLMOps Eval
    run_id = f"rag-demo-{datetime.now().strftime('%Y%m%d-%H%M%S')}"

    print("Submitting to LLMOps Eval platform...")
    with httpx.Client(timeout=60) as client:
        project_id = get_or_create_project(client)
        print(f"Using project: {project_id}")

        eval_payload = {
            "projectId": project_id,
            "name": f"RAG Demo Evaluation — {datetime.now().strftime('%Y-%m-%d %H:%M')}",
            "evaluationMode": "PRE_GENERATED",
            "metrics": metrics_payload,
            "testCases": test_case_payloads,
        }

        # Add judge endpoint if using LLM-based metrics
        judge_metrics = {"FAITHFULNESS", "CONTEXT_RECALL", "CONTEXT_PRECISION",
                         "ANSWER_RELEVANCY", "RELEVANCE", "ANSWER_CORRECTNESS", "FLUENCY"}
        needs_judge = any(m in judge_metrics for m in args.metrics)
        if needs_judge and not args.mock and api_key and not api_key.startswith("mock"):
            eval_payload["judgeEndpoint"] = {
                "providerType": "OPENAI",
                "modelName": "gpt-4o-mini",
                "apiKey": api_key,
            }
        elif needs_judge and args.mock:
            print("  Note: LLM-based metrics (FAITHFULNESS etc.) need a judge LLM.")
            print("  In mock mode, these will score at 0.5 (default on missing judge).")
            print()

        resp = client.post(
            f"{LLMOPS_API}/evaluations",
            headers=auth_headers(),
            json=eval_payload,
        )
        resp.raise_for_status()
        actual_run_id = resp.json()["evaluationRunId"]
        print(f"Evaluation submitted. Run ID: {actual_run_id}")

        # 5. Poll for completion
        print("Waiting for results", end="", flush=True)
        for _ in range(120):  # max 4 minutes
            time.sleep(2)
            status_resp = client.get(
                f"{LLMOPS_API}/evaluations/{actual_run_id}/status",
                headers=auth_headers(),
            ).json()
            status = status_resp.get("status", "PENDING")
            print(".", end="", flush=True)
            if status in ("COMPLETED", "FAILED"):
                break
        print()

        if status == "FAILED":
            print("ERROR: Evaluation failed. Check eval-engine logs.")
            sys.exit(1)

        # 6. Fetch results
        results = client.get(
            f"{LLMOPS_API}/evaluations/{actual_run_id}/results",
            headers=auth_headers(),
        ).json()

    # 7. Print report
    _print_report(results, GOLDEN_TEST_CASES)


def _print_report(results: dict, test_cases: list):
    try:
        from rich.console import Console
        from rich.table import Table
        from rich import box
        console = Console()
        _rich_report(console, results, test_cases)
    except ImportError:
        _plain_report(results, test_cases)


def _rich_report(console, results: dict, test_cases: list):
    from rich.table import Table
    from rich.panel import Panel
    from rich import box

    overall = results.get("overallScore")
    completed = results.get("completedCount", 0)
    failed_count = results.get("failedCount", 0)
    cost = results.get("totalCostUsd", 0.0)

    console.print()
    console.print(Panel.fit(
        f"[bold]RAG Evaluation Report[/bold]\n"
        f"Overall Score: [{'green' if overall and overall >= 0.75 else 'red'}]{overall:.3f}[/]\n"
        f"Completed: {completed} / {completed + failed_count}   "
        f"Cost: ${cost:.4f}",
        title="LLMOps Eval Results",
    ))

    # Metric summary table
    table = Table(title="Metric Summaries", box=box.ROUNDED)
    table.add_column("Metric",          style="cyan",  no_wrap=True)
    table.add_column("Avg Score",       justify="right")
    table.add_column("Min",             justify="right")
    table.add_column("Max",             justify="right")
    table.add_column("Pass Rate",       justify="right")
    table.add_column("Threshold",       justify="right")

    threshold_map = {
        "ROUGE_L": 0.35, "SEMANTIC_SIMILARITY": 0.75, "FAITHFULNESS": 0.80,
        "CONTEXT_RECALL": 0.75, "RELEVANCE": 0.78, "ANSWER_CORRECTNESS": 0.75,
    }

    for ms in results.get("metricSummaries", []):
        avg = ms.get("averageScore", 0)
        threshold = threshold_map.get(ms["metricCode"], None)
        pass_rate = ms.get("passRate")
        color = "green" if threshold is None or avg >= threshold else "red"

        table.add_row(
            ms["metricCode"],
            f"[{color}]{avg:.3f}[/]",
            f"{ms.get('minScore', 0):.3f}",
            f"{ms.get('maxScore', 0):.3f}",
            f"{pass_rate:.0%}" if pass_rate is not None else "—",
            f"{threshold:.2f}" if threshold else "—",
        )

    console.print(table)

    # Per test-case table
    tc_map = {tc["id"]: tc for tc in test_cases}
    case_table = Table(title="Per Test Case Results", box=box.SIMPLE)
    case_table.add_column("ID",          style="dim", no_wrap=True)
    case_table.add_column("Question",    max_width=40)
    case_table.add_column("Answer preview", max_width=40)
    case_table.add_column("Top metric score", justify="right")
    case_table.add_column("Status")

    for r in results.get("results", []):
        tc = tc_map.get(r["testCaseId"], {})
        scores = r.get("scores", [])
        best = max(scores, key=lambda s: s.get("score", 0)) if scores else {}
        score_val = best.get("score", 0)
        status = "✅" if score_val >= 0.6 else "⚠️" if score_val >= 0.4 else "❌"
        case_table.add_row(
            r["testCaseId"],
            tc.get("question", "")[:40],
            (r.get("generatedOutput") or "")[:40],
            f"{score_val:.3f} ({best.get('metricCode','?')})",
            status,
        )

    console.print(case_table)

    # Highlight hallucination test
    hallucination_result = next(
        (r for r in results.get("results", []) if "hallucination" in r.get("testCaseId", "")),
        None,
    )
    if hallucination_result:
        faith_score = next(
            (s for s in hallucination_result.get("scores", []) if s.get("metricCode") == "FAITHFULNESS"),
            None,
        )
        if faith_score:
            score = faith_score.get("score", 0)
            color = "green" if score < 0.3 else "red"
            console.print(
                f"\n[bold]Hallucination Test (rag_012):[/bold] "
                f"FAITHFULNESS = [{color}]{score:.3f}[/] "
                f"({'✅ Correctly detected' if score < 0.3 else '❌ Hallucination NOT detected'})"
            )

    console.print(f"\n[dim]Full results: http://localhost:3000/evaluations/{results.get('evaluationRunId')}[/dim]")


def _plain_report(results: dict, test_cases: list):
    print("\n" + "=" * 60)
    print("RAG EVALUATION RESULTS")
    print("=" * 60)
    print(f"Overall Score : {results.get('overallScore', 0):.3f}")
    print(f"Completed     : {results.get('completedCount', 0)} / {results.get('totalTestCases', 0)}")
    print(f"Failed        : {results.get('failedCount', 0)}")
    print(f"Cost          : ${results.get('totalCostUsd', 0):.4f}")
    print()
    print("METRIC SUMMARIES:")
    for ms in results.get("metricSummaries", []):
        print(f"  {ms['metricCode']:30s}  avg={ms.get('averageScore',0):.3f}  "
              f"pass_rate={ms.get('passRate') or 'N/A'}")
    print()
    print(f"View full results at: http://localhost:3000/evaluations/{results.get('evaluationRunId')}")


if __name__ == "__main__":
    main()
