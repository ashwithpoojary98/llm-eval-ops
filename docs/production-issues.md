# Production Issues Audit Report

Audit date: 2026-03-21. Issues are listed by severity. Fixed items are marked ✅.

---

## CRITICAL

### 1. ✅ Unauthenticated internal callback endpoint
**File:** `EvaluationController.java:205`
**Problem:** `POST /api/evaluations/callback/{id}` had no auth whatsoever. Any anonymous HTTP client could call it to corrupt evaluation results, inject fake scores, or flip any evaluation to COMPLETED/FAILED.
**Fix:** Added `X-Callback-Secret` header check. Backend rejects the request with 401 if the header is missing or wrong. Both sides read from `EVALUATION_CALLBACK_SECRET` env var.

### 2. ✅ CORS hardcoded to localhost
**File:** `SecurityConfig.java:61`
**Problem:** `configuration.setAllowedOrigins(List.of("http://localhost:3000"))` — changing the frontend domain required a code change and rebuild.
**Fix:** Reads from `${CORS_ALLOWED_ORIGINS:http://localhost:3000}`. Set `CORS_ALLOWED_ORIGINS=https://app.yourdomain.com` in prod.

### 3. ✅ DEBUG log level leaking internal data
**File:** `application.yaml:77`
**Problem:** `io.github.ashwithpoojary98: DEBUG` logs every SQL query, HTTP request body, and token. In production this fills logs with PII and secrets.
**Fix:** Changed to `${LOG_LEVEL:INFO}` — defaults to INFO, set `LOG_LEVEL=DEBUG` only locally.

### 4. ✅ Raw request body logged on validation errors
**File:** `evaluation-engine/app/main.py:87`
**Problem:** `body=body.decode()` logged the full request body on validation failure — which can include LLM API keys, prompts, or personal data.
**Fix:** Now logs only field names, error types, and message — never the payload.

---

## HIGH

### 5. ✅ `/api/invitations/**` fully open to unauthenticated access
**File:** `SecurityConfig.java:46`
**Problem:** `.requestMatchers("/api/invitations/**").permitAll()` — listing, revoking, and modifying invitations required no auth, allowing enumeration and manipulation.
**Fix:** Only `/api/invitations/accept/**` and `/api/invitations/validate/**` are public (needed for email-link flows). All other invitation operations require authentication.

### 6. ✅ No Kubernetes NetworkPolicy — all pods could talk to all pods
**File:** `k8s/base/` — missing
**Problem:** If any pod is compromised, an attacker could directly reach the Postgres pod from any other pod in the namespace.
**Fix:** Created `k8s/base/network-policy.yaml` with a default-deny-ingress policy and explicit allow rules:
- Frontend → only reachable from ingress-nginx
- Backend → reachable from ingress-nginx + frontend + eval-engine (for callbacks)
- Eval engine → reachable from backend only
- Postgres → reachable from backend + eval-engine only

### 7. ✅ ConfigMap with hardcoded `localhost:3000` as base URL
**File:** `k8s/base/backend.yaml:16`
**Problem:** `LLMOPS_BASE_URL: "http://localhost:3000"` — this is embedded in email links, platform branding, and OAuth redirects. In production it pointed to the wrong host.
**Fix:** Changed to `https://llmops.yourdomain.com` with a comment to update in the prod overlay.

### 8. ✅ Eval engine callback sends no authentication
**File:** `evaluation-engine/app/services/evaluation_service.py`
**Problem:** Outbound callback HTTP calls had no auth headers — any man-in-the-middle or SSRF could forge callback responses.
**Fix:** Reads `settings.callback_secret` and adds `X-Callback-Secret` header to every outbound callback.

---

## MEDIUM — Action Required Before Production

### 9. JWT tokens in localStorage (XSS risk)
**File:** `frontend/src/lib/api.ts:23`
**Problem:** `localStorage.getItem("accessToken")` — localStorage is readable by any JavaScript on the page. An XSS vulnerability anywhere in the app exposes all tokens.
**Recommended fix:** Switch to httpOnly cookies. Requires:
1. Backend sets `Set-Cookie: accessToken=...; HttpOnly; Secure; SameSite=Strict`
2. Frontend removes all `localStorage` token calls and relies on cookies being sent automatically
**Not fixed now** because it is a larger refactor, but it is the highest-priority remaining item.

### 10. Weak default secrets in application.yaml
**File:** `application.yaml:7,43,72`
**Problem:** `DATABASE_PASSWORD:1234`, `JWT_SECRET_KEY:change-this-...`, `APP_ENCRYPTION_KEY:change-this-...` — if env vars are not set, the app starts with known-weak credentials.
**Recommended fix:** Add a startup validation bean that refuses to start if these contain the default placeholder values:
```java
@Component
public class SecretValidator implements ApplicationListener<ApplicationReadyEvent> {
    @Value("${jwt.secret-key}") private String jwtSecret;
    @Value("${app.encryption.key}") private String encKey;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (jwtSecret.startsWith("change-this") || encKey.startsWith("change-this")) {
            throw new IllegalStateException(
                "STARTUP FAILED: Default placeholder secrets detected. " +
                "Set JWT_SECRET_KEY and APP_ENCRYPTION_KEY env vars to strong random values."
            );
        }
    }
}
```

### 11. No rate limiting on auth endpoints
**Problem:** `POST /api/auth/login` and `POST /api/auth/forgot-password` have no rate limiting — brute-force and email enumeration attacks are trivial.
**Recommended fix:** Add Bucket4j (Spring Boot rate limiting library):
```xml
<dependency>
  <groupId>com.giffing.bucket4j.spring.boot.starter</groupId>
  <artifactId>bucket4j-spring-boot-starter</artifactId>
</dependency>
```
```yaml
bucket4j:
  filters:
    - cache-name: rate-limit
      url: /api/auth/login
      rate-limits:
        - bandwidths:
            - capacity: 10
              time: 1
              unit: minutes
```

### 12. No Kubernetes ResourceQuota
**File:** `k8s/base/namespace.yaml`
**Problem:** No resource quota on the namespace — a misbehaving pod (e.g., eval engine running an expensive BERTScore job) can consume all cluster memory and starve other pods.
**Recommended fix:** Add to `namespace.yaml`:
```yaml
---
apiVersion: v1
kind: ResourceQuota
metadata:
  name: llmops-quota
  namespace: llmops
spec:
  hard:
    requests.cpu: "8"
    requests.memory: 16Gi
    limits.cpu: "24"
    limits.memory: 32Gi
    pods: "30"
```

### 13. Kubernetes Secrets in git (base64 is not encryption)
**File:** `k8s/base/secrets.yaml`
**Problem:** Secrets are committed as base64 — easily decoded with `echo <value> | base64 -d`.
**Recommended fix (pick one):**
- **Sealed Secrets** (easiest): `kubeseal` encrypts secrets with the cluster's public key. Only the cluster can decrypt.
- **External Secrets Operator**: reads from AWS Secrets Manager, HashiCorp Vault, GCP Secret Manager
- **SOPS**: encrypts individual values in YAML with GPG or cloud KMS

### 14. eval-engine PostgreSQL default password mismatch
**File:** `evaluation-engine/app/config.py:29`
**Problem:** Default `database_url` uses password `postgres` but docker-compose uses `${POSTGRES_PASSWORD}`. If `.env` is not set, the engine connects with wrong credentials.
**Recommended fix:** Remove the default password from the default URL:
```python
database_url: str = "postgresql+asyncpg://postgres:postgres@localhost:5432/llmops_eval"
# → Change to require explicit DATABASE_URL in all environments
```

### 15. FastAPI `/docs` and `/redoc` exposed in production
**File:** `evaluation-engine/app/main.py:62`
**Problem:** FastAPI's auto-generated Swagger UI is enabled by default and documents your internal evaluation API to anyone who can reach port 8000. Since the eval engine should only be accessible internally, this leaks API structure.
**Recommended fix:**
```python
app = FastAPI(
    ...
    docs_url="/docs" if settings.debug else None,
    redoc_url="/redoc" if settings.debug else None,
    openapi_url="/openapi.json" if settings.debug else None,
)
```

---

## LOW — Good to Have

| # | Issue | File | Recommendation |
|---|-------|------|----------------|
| 16 | No K8s liveness probe on eval-engine | `k8s/base/evaluation-engine.yaml` | Add `livenessProbe` on `/health/live` |
| 17 | Python deps not pinned exactly | `evaluation-engine/requirements.txt` | Run `pip freeze > requirements-lock.txt` and use that in Dockerfile |
| 18 | No DB backup strategy in K8s | `k8s/base/postgres.yaml` | Add a CronJob that runs `pg_dump` and uploads to S3 |
| 19 | Spring Security logs at INFO (noisy) | `application.yaml` | Already fixed to WARN |
| 20 | No Content-Security-Policy header | `frontend/next.config.js` | Add `headers()` in next.config.js with `Content-Security-Policy`, `X-Frame-Options`, `X-Content-Type-Options` |

---

## Security Checklist — Before Going Live

```
[ ] Generate JWT_SECRET_KEY   (openssl rand -hex 32)
[ ] Generate APP_ENCRYPTION_KEY  (openssl rand -hex 16)
[ ] Generate EVALUATION_CALLBACK_SECRET  (openssl rand -hex 32)
[ ] Set strong POSTGRES_PASSWORD
[ ] Set CORS_ALLOWED_ORIGINS to your actual domain
[ ] Set LOG_LEVEL=INFO (not DEBUG)
[ ] Remove postgres port mapping from docker-compose (5432:5432) in production
[ ] Disable FastAPI /docs in production (see issue #15)
[ ] Migrate tokens from localStorage to httpOnly cookies (see issue #9)
[ ] Add Bucket4j rate limiting to auth endpoints (see issue #11)
[ ] Use Sealed Secrets or External Secrets for K8s (see issue #13)
[ ] Verify NetworkPolicy blocks are working: kubectl exec a frontend pod and confirm it cannot curl postgres:5432
```
