# ── Stage 1: Build dependencies in a virtual environment ─────────────────────
FROM python:3.11-slim AS builder

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Create a venv — avoids /root/.local permission issues for non-root runtime user
RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Download NLTK data into a known location that we can copy to the runtime stage
RUN python -c "import nltk; nltk.download('punkt', quiet=True); nltk.download('punkt_tab', quiet=True); nltk.download('wordnet', quiet=True)" \
    && mv /root/nltk_data /opt/nltk_data

# ── Stage 2: Minimal runtime image ────────────────────────────────────────────
FROM python:3.11-slim

WORKDIR /app

# Copy venv and NLTK data from builder
COPY --from=builder /opt/venv /opt/venv
COPY --from=builder /opt/nltk_data /opt/nltk_data

# Point to the venv and NLTK data
ENV PATH="/opt/venv/bin:$PATH"
ENV NLTK_DATA="/opt/nltk_data"

# Non-root user
RUN useradd -m -u 1000 appuser && chown -R appuser:appuser /app
USER appuser

COPY --chown=appuser:appuser app/ ./app/

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health/live')" || exit 1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
