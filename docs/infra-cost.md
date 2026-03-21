# Infrastructure Cost Guide

Exact monthly cost estimates for every tier — from a $0 laptop to a production cluster.

---

## Tier 0 — Local Development (Free)

Run everything on your machine using Docker Compose.

```bash
cp .env.example .env          # fill in your LLM API keys
docker compose up -d          # starts postgres + backend + eval-engine + frontend
```

**What you get:**
- Full platform running locally
- PostgreSQL with persistent volume
- All 25 metrics available
- No LLM API cost unless you use LIVE_GENERATION mode

**Cost: $0/month** (electricity aside)

---

## Tier 1 — Single Server (Small Team / Startup)

One VM running Docker Compose. Good for teams up to ~20 users running a few hundred evaluations per week.

### Hetzner CX32 (best value, Europe)

| Resource      | Spec              | Cost/month |
|---------------|-------------------|------------|
| VM (CX32)     | 4 vCPU · 8 GB RAM | €14.00     |
| Block storage | 80 GB SSD         | €4.00      |
| Backups       | 20% of VM price   | €2.80      |
| **Total**     |                   | **~€21/month (~$23)** |

### DigitalOcean (US/global)

| Resource         | Spec              | Cost/month |
|------------------|-------------------|------------|
| Droplet          | 4 vCPU · 8 GB RAM | $48.00     |
| Managed Postgres | 1 node, 1 GB RAM  | $15.00     |
| Backups          | 20% of Droplet    | $9.60      |
| **Total**        |                   | **~$73/month** |

### AWS (t3.large + RDS)

| Resource   | Spec                       | Cost/month |
|------------|----------------------------|------------|
| EC2 t3.large | 2 vCPU · 8 GB RAM        | $60.00     |
| RDS db.t3.micro (Postgres) | 2 GB RAM  | $28.00     |
| EBS 50 GB  | gp3                        | $4.00      |
| **Total**  |                            | **~$92/month** |

### What handles at this tier
- 50-100 concurrent users browsing the UI
- ~500 evaluations/week (each with 100 test cases)
- Eval engine: ~2,000 metric calculations/hour
- Backend + frontend + eval-engine all on one box

---

## Tier 2 — Kubernetes Cluster (Growing Company)

Use the `k8s/overlays/prod` Kustomize overlay. Proper HA, auto-scaling, TLS.

### GKE Autopilot (simplest managed K8s)

| Resource                    | Spec                    | Cost/month |
|-----------------------------|-------------------------|------------|
| GKE Autopilot nodes         | 3 × e2-standard-2       | $120.00    |
| Cloud SQL Postgres          | db-g1-small, 10 GB      | $45.00     |
| Load Balancer               | 1 regional              | $18.00     |
| Cloud Storage (backups)     | 50 GB                   | $1.00      |
| **Total**                   |                         | **~$184/month** |

### EKS on AWS (most enterprise-common)

| Resource              | Spec                  | Cost/month |
|-----------------------|-----------------------|------------|
| EKS control plane     | managed               | $73.00     |
| EC2 nodes (×3)        | t3.medium (2vCPU/4GB) | $90.00     |
| RDS Postgres          | db.t3.small, Multi-AZ | $68.00     |
| ALB                   | 1 Application LB      | $22.00     |
| EBS volumes           | 100 GB gp3            | $8.00      |
| **Total**             |                       | **~$261/month** |

### AKS on Azure (if you're already in Azure)

| Resource            | Spec                    | Cost/month |
|---------------------|-------------------------|------------|
| AKS node pool       | 3 × Standard_B2s        | $88.00     |
| Azure Database      | Postgres Flexible B1ms  | $38.00     |
| Azure LB + Public IP|                         | $14.00     |
| **Total**           |                         | **~$140/month** |

### What handles at this tier
- HPA auto-scales backend: 2-6 pods
- HPA auto-scales eval-engine: 2-8 pods
- Frontend: 2 pods (mostly static)
- ~5,000 evaluations/week
- Database connection pooling via PgBouncer (already in compose)
- Rolling deployments with zero downtime

---

## Tier 3 — Production-Grade (Enterprise)

Multi-region, dedicated database cluster, observability stack.

| Resource                      | Spec                              | Cost/month |
|-------------------------------|-----------------------------------|------------|
| EKS nodes (×6, 2 AZ)          | m5.xlarge (4vCPU/16GB)            | $720.00    |
| RDS Aurora Postgres (Multi-AZ) | db.r6g.large writer + 1 reader   | $480.00    |
| ElastiCache Redis              | cache.t3.small (session cache)    | $25.00     |
| ALB + WAF                     | Web Application Firewall          | $55.00     |
| CloudWatch / Datadog           | Monitoring + alerting             | $60.00     |
| S3 backups                    | Daily DB snapshots                | $5.00      |
| **Total**                     |                                   | **~$1,345/month** |

### What handles at this tier
- 50,000+ evaluations/week
- Multi-tenant isolation with row-level security
- 99.9% uptime SLA possible
- WAF protecting against injection/scraping
- Full audit trail for SOC2/HIPAA compliance

---

## LLM API Cost (the variable part)

The platform itself is cheap. The cost you actually feel is LLM API calls for LIVE_GENERATION mode and LLM-as-Judge metrics.

### Per evaluation run (100 test cases, 3 judge metrics)

| Judge Model       | Input tokens | Output tokens | Cost/run |
|-------------------|-------------|---------------|----------|
| GPT-4o            | ~60K        | ~15K          | $0.41    |
| GPT-4o-mini       | ~60K        | ~15K          | $0.029   |
| Claude 3.5 Haiku  | ~60K        | ~15K          | $0.024   |
| Claude 3.5 Sonnet | ~60K        | ~15K          | $0.36    |

**Recommendation:** Use GPT-4o-mini or Claude 3.5 Haiku as your judge for daily runs. Use GPT-4o or Claude 3.5 Sonnet only for monthly deep audits.

### Non-LLM metrics are free

BLEU, ROUGE, METEOR, Semantic Similarity (sentence-transformers runs locally), BERTScore — these run entirely on your cluster CPU. Zero API cost.

---

## Cost vs SaaS Alternatives

| Tool           | Monthly cost (10 users, 5K evals/week) | Data leaves your infra? |
|----------------|----------------------------------------|-------------------------|
| LangSmith      | ~$390 ($39/user)                       | Yes — US cloud          |
| W&B Weave      | ~$500 ($50/user)                       | Yes — US cloud          |
| PromptLayer    | ~$290 (Pay-per-request)               | Yes — US cloud          |
| **LLMOps Eval (K8s tier 2)** | **~$200 infra + ~$30 LLM API = $230** | **No — your servers** |

At 10 users you break even in month 1. At 20 users you save $400-600/month forever.

---

## Scaling Cheat Sheet

| You need to scale when...               | What to adjust                          |
|-----------------------------------------|-----------------------------------------|
| Evaluations are slow (queue backing up) | Increase `MAX_PARALLEL_EVALUATIONS` env var; add eval-engine replicas |
| DB connections exhausted                | Enable PgBouncer sidecar; increase `DB_POOL_SIZE` |
| Frontend is slow                        | Add CDN (Cloudflare free tier) in front |
| Backend OOM                             | Increase `memory` in `k8s/overlays/prod/patch-resources.yaml` |
| LLM API costs too high                  | Switch judge to GPT-4o-mini; increase batch size |
