"""
Simple RAG pipeline for demo purposes.

- Documents: plain text files in sample_docs/
- Chunking: paragraph-level splitting
- Retrieval: cosine similarity on sentence-transformer embeddings (runs locally, free)
- Generation: OpenAI GPT-4o-mini (configurable; falls back to mock if no key)
"""
import os
import re
import time
from pathlib import Path
from typing import Optional

import numpy as np

# ── Document loading ──────────────────────────────────────────────────────────

def load_documents(docs_dir: str = "sample_docs") -> list[dict]:
    """Load all .txt files and split into paragraph chunks."""
    docs = []
    for path in Path(docs_dir).glob("*.txt"):
        text = path.read_text(encoding="utf-8")
        paragraphs = [p.strip() for p in re.split(r"\n{2,}", text) if p.strip()]
        for i, para in enumerate(paragraphs):
            docs.append({
                "id": f"{path.stem}_{i}",
                "source": path.name,
                "content": para,
            })
    print(f"Loaded {len(docs)} chunks from {docs_dir}/")
    return docs


# ── Embedding + retrieval ─────────────────────────────────────────────────────

_embedding_model = None

def get_embedding_model():
    global _embedding_model
    if _embedding_model is None:
        from sentence_transformers import SentenceTransformer
        print("Loading embedding model (first time takes ~30 seconds)...")
        _embedding_model = SentenceTransformer("all-MiniLM-L6-v2")
    return _embedding_model


def embed_documents(docs: list[dict]) -> np.ndarray:
    model = get_embedding_model()
    texts = [d["content"] for d in docs]
    return model.encode(texts, convert_to_numpy=True, show_progress_bar=False)


def retrieve(
    query: str,
    docs: list[dict],
    doc_embeddings: np.ndarray,
    top_k: int = 3,
) -> list[dict]:
    """Return top_k most relevant chunks for a query."""
    model = get_embedding_model()
    query_emb = model.encode([query], convert_to_numpy=True)[0]

    # Cosine similarity
    norms = np.linalg.norm(doc_embeddings, axis=1, keepdims=True)
    safe_norms = np.where(norms == 0, 1, norms)
    normalized = doc_embeddings / safe_norms
    query_norm = query_emb / (np.linalg.norm(query_emb) or 1)
    scores = normalized @ query_norm

    top_idx = np.argsort(scores)[::-1][:top_k]
    return [
        {**docs[i], "similarity_score": float(scores[i])}
        for i in top_idx
    ]


# ── LLM generation ────────────────────────────────────────────────────────────

def generate_answer(
    question: str,
    context_chunks: list[dict],
    api_key: Optional[str] = None,
    model: str = "gpt-4o-mini",
) -> tuple[str, float, int]:
    """
    Generate an answer using OpenAI.
    Returns: (answer_text, latency_ms, total_tokens)

    Falls back to a mock answer if no API key provided (for testing without spend).
    """
    context_text = "\n\n".join(
        f"[Source: {c['source']}]\n{c['content']}"
        for c in context_chunks
    )

    # ── Mock mode (no API key needed) ────────────────────────────────────────
    if not api_key or api_key.startswith("mock"):
        time.sleep(0.05)  # Simulate latency
        # Simple keyword-based mock — useful for testing the eval pipeline
        answer = _mock_answer(question, context_chunks)
        return answer, 50, 120

    # ── Real OpenAI call ─────────────────────────────────────────────────────
    from openai import OpenAI
    client = OpenAI(api_key=api_key)

    prompt = f"""You are a helpful customer support assistant. Answer the user's question using ONLY the provided context. If the answer is not in the context, say "I don't have that information."

Context:
{context_text}

Question: {question}

Answer:"""

    start = time.time()
    response = client.chat.completions.create(
        model=model,
        messages=[{"role": "user", "content": prompt}],
        temperature=0.0,
        max_tokens=300,
    )
    latency_ms = int((time.time() - start) * 1000)

    answer = response.choices[0].message.content or ""
    tokens = response.usage.total_tokens if response.usage else 0
    return answer, latency_ms, tokens


def _mock_answer(question: str, context_chunks: list[dict]) -> str:
    """Keyword-based mock for when no OpenAI key is available."""
    q = question.lower()
    combined = " ".join(c["content"] for c in context_chunks)

    if "refund" in q:
        return "We offer full refunds within 14 days of purchase for annual plans. Monthly plans are non-refundable."
    elif "cancel" in q:
        return "Go to Settings > Subscription > Cancel Plan. Your access continues until the end of your current billing cycle."
    elif "password" in q:
        return "Click 'Forgot Password' on the login page. You will receive a reset link within 5 minutes."
    elif "rate limit" in q or "api" in q:
        return "Rate limits depend on your plan: Free 100/hour, Starter 1,000/hour, Pro 10,000/hour."
    elif "support" in q or "contact" in q:
        return "Pro users can email support@techcorp.com with a 24-hour response SLA."
    elif "encrypt" in q or "security" in q:
        return "Data is encrypted in transit using TLS 1.3 and at rest using AES-256."
    elif "soc" in q or "compliant" in q:
        return "Yes, we are SOC 2 Type II certified."
    elif "free" in q and "tier" in q:
        return "The Free tier includes 100 API calls/hour, 1 project, 5 GB storage, and community support."
    else:
        # Extract first sentence from top context chunk as fallback
        first_chunk = context_chunks[0]["content"] if context_chunks else ""
        sentences = first_chunk.split(". ")
        return sentences[0] + "." if sentences else "I don't have that information."


# ── RAG pipeline (end-to-end) ─────────────────────────────────────────────────

class RAGPipeline:
    def __init__(
        self,
        docs_dir: str = "sample_docs",
        openai_api_key: Optional[str] = None,
        model: str = "gpt-4o-mini",
        top_k: int = 3,
    ):
        self.api_key = openai_api_key
        self.model = model
        self.top_k = top_k

        print("\n=== Initializing RAG Pipeline ===")
        self.docs = load_documents(docs_dir)
        print("Building document index...")
        self.doc_embeddings = embed_documents(self.docs)
        print("Pipeline ready.\n")

    def query(self, question: str) -> dict:
        """Run a single question through the RAG pipeline."""
        # Retrieve
        retrieved = retrieve(question, self.docs, self.doc_embeddings, self.top_k)

        # Generate
        answer, latency_ms, tokens = generate_answer(
            question=question,
            context_chunks=retrieved,
            api_key=self.api_key,
            model=self.model,
        )

        return {
            "question": question,
            "answer": answer,
            "retrieved_context": [c["content"] for c in retrieved],
            "latency_ms": latency_ms,
            "tokens": tokens,
            "top_sources": [c["source"] for c in retrieved],
        }
