"""
FastAPI Application Entry Point
"""
import structlog
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError

from app.config import get_settings
from app.database import init_db
from app.api.routes import health, evaluation, metrics as metrics_routes

# Configure structured logging
structlog.configure(
    processors=[
        structlog.stdlib.filter_by_level,
        structlog.stdlib.add_logger_name,
        structlog.stdlib.add_log_level,
        structlog.stdlib.PositionalArgumentsFormatter(),
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.processors.UnicodeDecoder(),
        structlog.processors.JSONRenderer()
    ],
    wrapper_class=structlog.stdlib.BoundLogger,
    context_class=dict,
    logger_factory=structlog.stdlib.LoggerFactory(),
    cache_logger_on_first_use=True,
)

logger = structlog.get_logger()
settings = get_settings()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan handler for startup and shutdown."""
    # Startup
    logger.info("Starting evaluation engine", version=settings.app_version)
    await init_db()
    logger.info("Database initialized")

    # Download NLTK data if needed
    try:
        import nltk
        nltk.download('punkt', quiet=True)
        nltk.download('punkt_tab', quiet=True)
        nltk.download('wordnet', quiet=True)
        logger.info("NLTK data downloaded")
    except Exception as e:
        logger.warning("Failed to download NLTK data", error=str(e))

    yield

    # Shutdown
    logger.info("Shutting down evaluation engine")


app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="Evaluation engine for LLM and RAG systems",
    lifespan=lifespan,
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Validation error handler with detailed logging
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    # Log field names and error types only — never the raw request body (may contain secrets/PII)
    safe_errors = [
        {"field": ".".join(str(l) for l in e.get("loc", [])), "type": e.get("type"), "msg": e.get("msg")}
        for e in exc.errors()
    ]
    logger.error(
        "Validation error",
        path=str(request.url.path),
        method=request.method,
        errors=safe_errors,
    )
    return JSONResponse(
        status_code=422,
        content={"detail": exc.errors()},
    )


# Include routers
app.include_router(health.router)
app.include_router(evaluation.router)
app.include_router(metrics_routes.router)


@app.get("/")
async def root():
    """Root endpoint with API information."""
    return {
        "name": settings.app_name,
        "version": settings.app_version,
        "docs": "/docs",
        "health": "/health",
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=settings.debug,
    )
