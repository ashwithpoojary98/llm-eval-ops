"""
Evaluation API endpoints.
"""
import structlog
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, BackgroundTasks
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models.requests import EvaluationRequest
from app.models.responses import (
    EvaluationQueuedResponse,
    EvaluationStatusResponse,
    EvaluationResponse,
    TestCaseResult,
    MetricResult,
    MetricSummary,
)
from app.models.enums import EvaluationStatus, TaskStatus, MetricCode
from app.models.db_models import EvaluationTask, EvaluationResult, MetricScore
from app.services.evaluation_service import EvaluationService

router = APIRouter(prefix="/evaluate", tags=["evaluation"])
logger = structlog.get_logger()


@router.post("", response_model=EvaluationQueuedResponse)
async def start_evaluation(
    request: EvaluationRequest,
    background_tasks: BackgroundTasks,
    db: AsyncSession = Depends(get_db),
):
    """
    Start a new evaluation run.

    The evaluation runs asynchronously in the background.
    Use the status endpoint to check progress.
    """
    logger.info(
        "Starting evaluation",
        evaluation_run_id=request.evaluation_run_id,
        test_case_count=len(request.test_cases),
        metrics=[m.code.value for m in request.metrics],
    )

    # Check if evaluation already exists
    existing = await db.execute(
        select(EvaluationTask).where(
            EvaluationTask.evaluation_run_id == request.evaluation_run_id
        )
    )
    if existing.scalar_one_or_none():
        raise HTTPException(
            status_code=409,
            detail=f"Evaluation {request.evaluation_run_id} already exists"
        )

    # Create task record
    task = EvaluationTask(
        evaluation_run_id=request.evaluation_run_id,
        project_id=request.project_id,
        status=EvaluationStatus.PENDING.value,
        evaluation_mode=request.evaluation_mode.value,
        total_test_cases=len(request.test_cases),
        callback_url=request.callback_url,
        created_at=datetime.utcnow(),
    )
    db.add(task)
    await db.commit()
    await db.refresh(task)

    # Queue background task
    evaluation_service = EvaluationService()
    background_tasks.add_task(
        evaluation_service.run_evaluation,
        request=request,
        task_id=str(task.id),
    )

    logger.info(
        "Evaluation queued",
        evaluation_run_id=request.evaluation_run_id,
        task_id=str(task.id),
    )

    return EvaluationQueuedResponse(
        evaluation_run_id=request.evaluation_run_id,
        status="queued",
        task_id=str(task.id),
        message=f"Evaluation queued with {len(request.test_cases)} test cases",
    )


@router.get("/{evaluation_run_id}/status", response_model=EvaluationStatusResponse)
async def get_evaluation_status(
    evaluation_run_id: str,
    db: AsyncSession = Depends(get_db),
):
    """Get the current status of an evaluation run."""
    result = await db.execute(
        select(EvaluationTask).where(
            EvaluationTask.evaluation_run_id == evaluation_run_id
        )
    )
    task = result.scalar_one_or_none()

    if not task:
        raise HTTPException(status_code=404, detail="Evaluation not found")

    # Map DB status to TaskStatus
    task_status_map = {
        EvaluationStatus.PENDING.value: TaskStatus.QUEUED,
        EvaluationStatus.RUNNING.value: TaskStatus.PROCESSING,
        EvaluationStatus.COMPLETED.value: TaskStatus.COMPLETED,
        EvaluationStatus.FAILED.value: TaskStatus.FAILED,
        EvaluationStatus.CANCELLED.value: TaskStatus.FAILED,
    }

    return EvaluationStatusResponse(
        evaluation_run_id=evaluation_run_id,
        status=EvaluationStatus(task.status),
        task_status=task_status_map.get(task.status, TaskStatus.QUEUED),
        progress={
            "completed": task.completed_count,
            "total": task.total_test_cases,
            "failed": task.failed_count,
        },
        started_at=task.started_at,
    )


@router.get("/{evaluation_run_id}/results", response_model=EvaluationResponse)
async def get_evaluation_results(
    evaluation_run_id: str,
    include_details: bool = True,
    db: AsyncSession = Depends(get_db),
):
    """Get the results of a completed evaluation run."""
    # Get task
    task_result = await db.execute(
        select(EvaluationTask).where(
            EvaluationTask.evaluation_run_id == evaluation_run_id
        )
    )
    task = task_result.scalar_one_or_none()

    if not task:
        raise HTTPException(status_code=404, detail="Evaluation not found")

    # Get results
    results_query = await db.execute(
        select(EvaluationResult).where(EvaluationResult.task_id == task.id)
    )
    results = results_query.scalars().all()

    # Build response
    test_case_results = []
    if include_details:
        for result in results:
            # Get scores for this result
            scores_query = await db.execute(
                select(MetricScore).where(MetricScore.result_id == result.id)
            )
            scores = scores_query.scalars().all()

            metric_results = [
                MetricResult(
                    metric_code=MetricCode(score.metric_code),
                    score=score.score,
                    raw_score=score.raw_score,
                    passed=score.passed,
                    details=score.details,
                    reasoning=score.reasoning,
                    error=score.error,
                )
                for score in scores
            ]

            test_case_results.append(
                TestCaseResult(
                    test_case_id=result.test_case_id,
                    status=result.status,
                    generated_output=result.generated_output,
                    llm_latency_ms=result.llm_latency_ms,
                    scores=metric_results,
                    error_message=result.error_message,
                    processing_time_ms=result.processing_time_ms,
                )
            )

    # Parse metric summaries from task
    metric_summaries = []
    if task.metric_summaries:
        for code, summary in task.metric_summaries.items():
            metric_summaries.append(
                MetricSummary(
                    metric_code=MetricCode(code),
                    average_score=summary.get("average", 0),
                    min_score=summary.get("min", 0),
                    max_score=summary.get("max", 0),
                    std_dev=summary.get("std_dev", 0),
                    pass_rate=summary.get("pass_rate"),
                    total_evaluated=summary.get("count", 0),
                )
            )

    return EvaluationResponse(
        evaluation_run_id=evaluation_run_id,
        status=EvaluationStatus(task.status),
        overall_score=task.overall_score,
        total_test_cases=task.total_test_cases,
        completed_count=task.completed_count,
        failed_count=task.failed_count,
        metric_summaries=metric_summaries,
        results=test_case_results,
        total_tokens_used=task.total_tokens,
        total_cost_usd=task.total_cost,
        started_at=task.started_at,
        completed_at=task.completed_at,
        error_message=task.error_message,
    )


@router.delete("/{evaluation_run_id}")
async def cancel_evaluation(
    evaluation_run_id: str,
    db: AsyncSession = Depends(get_db),
):
    """Cancel a running evaluation."""
    result = await db.execute(
        select(EvaluationTask).where(
            EvaluationTask.evaluation_run_id == evaluation_run_id
        )
    )
    task = result.scalar_one_or_none()

    if not task:
        raise HTTPException(status_code=404, detail="Evaluation not found")

    if task.status in [EvaluationStatus.COMPLETED.value, EvaluationStatus.FAILED.value]:
        raise HTTPException(
            status_code=400,
            detail=f"Cannot cancel evaluation with status {task.status}"
        )

    task.status = EvaluationStatus.CANCELLED.value
    task.completed_at = datetime.utcnow()
    await db.commit()

    logger.info("Evaluation cancelled", evaluation_run_id=evaluation_run_id)

    return {"status": "cancelled", "evaluation_run_id": evaluation_run_id}
