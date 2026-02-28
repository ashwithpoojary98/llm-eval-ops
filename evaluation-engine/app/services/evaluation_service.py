"""
Evaluation Service - orchestrates the evaluation process.
"""
import asyncio
import time
import statistics
from datetime import datetime
from typing import Optional
import structlog
import httpx

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import async_session_factory
from app.config import get_settings
from app.models.requests import EvaluationRequest, TestCaseInput, MetricConfig
from app.models.enums import EvaluationStatus, EvaluationMode, MetricCode
from app.models.db_models import EvaluationTask, EvaluationResult, MetricScore
from app.metrics.registry import get_metric_registry
from app.metrics.base import MetricResult as MetricCalcResult
from app.llm_clients.factory import create_llm_client
from app.llm_clients.base import BaseLLMClient

logger = structlog.get_logger()
settings = get_settings()


class EvaluationService:
    """
    Service that orchestrates evaluation runs.

    Handles:
    - Test case processing (sequential or parallel)
    - Metric calculation
    - LLM generation (for LIVE_GENERATION mode)
    - Result persistence
    - Progress tracking
    - Callback notifications
    """

    def __init__(self):
        self.metric_registry = get_metric_registry()

    async def run_evaluation(self, request: EvaluationRequest, task_id: str):
        """
        Run a complete evaluation.

        This is called as a background task.
        """
        logger.info(
            "Starting evaluation run",
            evaluation_run_id=request.evaluation_run_id,
            task_id=task_id,
            test_cases=len(request.test_cases),
            metrics=[m.code.value for m in request.metrics],
        )

        async with async_session_factory() as db:
            try:
                # Update task status to RUNNING
                await self._update_task_status(
                    db, task_id, EvaluationStatus.RUNNING,
                    started_at=datetime.utcnow()
                )

                # Initialize LLM clients if needed
                target_client = None
                judge_client = None

                if request.target_llm_endpoint:
                    target_client = create_llm_client(request.target_llm_endpoint)
                    logger.info("Target LLM client created", model=request.target_llm_endpoint.model_name)

                if request.judge_llm_endpoint:
                    judge_client = create_llm_client(request.judge_llm_endpoint)
                    logger.info("Judge LLM client created", model=request.judge_llm_endpoint.model_name)

                # Process test cases
                all_scores: dict[str, list[float]] = {m.code.value: [] for m in request.metrics}
                total_tokens = 0
                total_cost = 0.0
                completed = 0
                failed = 0

                # Process in batches for better performance
                batch_size = settings.batch_size

                for i in range(0, len(request.test_cases), batch_size):
                    batch = request.test_cases[i:i + batch_size]

                    # Process batch (could be parallelized further)
                    for test_case in batch:
                        try:
                            result = await self._process_test_case(
                                db=db,
                                task_id=task_id,
                                test_case=test_case,
                                metrics=request.metrics,
                                evaluation_mode=request.evaluation_mode,
                                target_client=target_client,
                                judge_client=judge_client,
                            )

                            # Aggregate scores
                            for score in result.get("scores", []):
                                if score["metric_code"] in all_scores:
                                    if score.get("score") is not None and score.get("error") is None:
                                        all_scores[score["metric_code"]].append(score["score"])

                            total_tokens += result.get("tokens", 0)
                            total_cost += result.get("cost", 0.0)
                            completed += 1

                        except Exception as e:
                            logger.error(
                                "Test case processing failed",
                                test_case_id=test_case.id,
                                error=str(e),
                            )
                            failed += 1

                            # Store failed result
                            await self._store_failed_result(
                                db, task_id, test_case.id, str(e)
                            )

                    # Update progress
                    await self._update_task_progress(
                        db, task_id, completed, failed
                    )

                # Calculate final metrics summaries
                metric_summaries = {}
                weighted_sum = 0.0
                total_weight = 0.0

                for metric_config in request.metrics:
                    code = metric_config.code.value
                    scores = all_scores.get(code, [])

                    if scores:
                        avg = statistics.mean(scores)
                        metric_summaries[code] = {
                            "average": round(avg, 4),
                            "min": round(min(scores), 4),
                            "max": round(max(scores), 4),
                            "std_dev": round(statistics.stdev(scores), 4) if len(scores) > 1 else 0.0,
                            "count": len(scores),
                            "pass_rate": round(
                                sum(1 for s in scores if s >= (metric_config.pass_threshold or 0)) / len(scores),
                                4
                            ) if metric_config.pass_threshold else None,
                        }

                        weighted_sum += avg * metric_config.weight
                        total_weight += metric_config.weight

                overall_score = weighted_sum / total_weight if total_weight > 0 else None

                # Update task with final results
                await self._finalize_task(
                    db=db,
                    task_id=task_id,
                    status=EvaluationStatus.COMPLETED,
                    overall_score=overall_score,
                    metric_summaries=metric_summaries,
                    total_tokens=total_tokens,
                    total_cost=total_cost,
                    completed_count=completed,
                    failed_count=failed,
                )

                logger.info(
                    "Evaluation completed",
                    evaluation_run_id=request.evaluation_run_id,
                    overall_score=overall_score,
                    completed=completed,
                    failed=failed,
                )

                # Send callback if configured
                if request.callback_url:
                    await self._send_callback(
                        request.callback_url,
                        {
                            "evaluation_run_id": request.evaluation_run_id,
                            "status": "COMPLETED",
                            "overall_score": overall_score,
                            "completed": completed,
                            "failed": failed,
                        }
                    )

            except Exception as e:
                logger.error(
                    "Evaluation failed",
                    evaluation_run_id=request.evaluation_run_id,
                    error=str(e),
                )

                await self._update_task_status(
                    db, task_id, EvaluationStatus.FAILED,
                    error_message=str(e),
                    completed_at=datetime.utcnow()
                )

                if request.callback_url:
                    await self._send_callback(
                        request.callback_url,
                        {
                            "evaluation_run_id": request.evaluation_run_id,
                            "status": "FAILED",
                            "error": str(e),
                        }
                    )

    async def _process_test_case(
        self,
        db: AsyncSession,
        task_id: str,
        test_case: TestCaseInput,
        metrics: list[MetricConfig],
        evaluation_mode: EvaluationMode,
        target_client: Optional[BaseLLMClient],
        judge_client: Optional[BaseLLMClient],
    ) -> dict:
        """Process a single test case."""
        start_time = time.time()

        generated_output = test_case.llm_output
        llm_latency_ms = None
        tokens_used = 0
        cost = 0.0

        # Live generation mode
        if evaluation_mode == EvaluationMode.LIVE_GENERATION:
            if not target_client:
                raise ValueError("Target LLM client required for LIVE_GENERATION mode")

            # Build prompt with context if available
            prompt = test_case.question
            if test_case.retrieved_context:
                context_text = "\n\n".join(test_case.retrieved_context)
                prompt = f"Context:\n{context_text}\n\nQuestion: {test_case.question}"

            # Generate response
            response = target_client.generate_with_metadata(prompt)
            generated_output = response.content
            llm_latency_ms = response.latency_ms
            tokens_used = response.total_tokens
            cost = response.metadata.get("cost_usd", 0.0)

        # Calculate metrics
        scores = []
        for metric_config in metrics:
            try:
                metric = self.metric_registry.get(
                    metric_config.code,
                    llm_client=judge_client,
                    prompt_template=metric_config.judge_prompt_template,
                )

                result = metric.calculate(
                    question=test_case.question,
                    generated_output=generated_output,
                    ground_truth=test_case.ground_truth,
                    context=test_case.retrieved_context,
                    **(metric_config.config or {}),
                )

                # Apply threshold if configured
                passed = None
                if metric_config.pass_threshold is not None:
                    passed = result.score >= metric_config.pass_threshold

                scores.append({
                    "metric_code": metric_config.code.value,
                    "score": result.score,
                    "raw_score": result.raw_score,
                    "passed": passed,
                    "details": result.details,
                    "reasoning": result.reasoning,
                    "error": result.error,
                })

            except Exception as e:
                logger.error(
                    "Metric calculation failed",
                    metric=metric_config.code.value,
                    test_case_id=test_case.id,
                    error=str(e),
                )
                scores.append({
                    "metric_code": metric_config.code.value,
                    "score": 0.0,
                    "raw_score": 0.0,
                    "error": str(e),
                })

        processing_time_ms = int((time.time() - start_time) * 1000)

        # Store result
        await self._store_result(
            db=db,
            task_id=task_id,
            test_case_id=test_case.id,
            generated_output=generated_output if evaluation_mode == EvaluationMode.LIVE_GENERATION else None,
            llm_latency_ms=llm_latency_ms,
            processing_time_ms=processing_time_ms,
            scores=scores,
            input_tokens=tokens_used // 2 if tokens_used else 0,  # Rough split
            output_tokens=tokens_used // 2 if tokens_used else 0,
        )

        return {
            "scores": scores,
            "tokens": tokens_used,
            "cost": cost,
        }

    async def _store_result(
        self,
        db: AsyncSession,
        task_id: str,
        test_case_id: str,
        generated_output: Optional[str],
        llm_latency_ms: Optional[int],
        processing_time_ms: int,
        scores: list[dict],
        input_tokens: int,
        output_tokens: int,
    ):
        """Store evaluation result for a test case."""
        result = EvaluationResult(
            task_id=task_id,
            test_case_id=test_case_id,
            status="COMPLETED",
            generated_output=generated_output,
            llm_latency_ms=llm_latency_ms,
            processing_time_ms=processing_time_ms,
            input_tokens=input_tokens,
            output_tokens=output_tokens,
        )
        db.add(result)
        await db.flush()

        # Store metric scores
        for score_data in scores:
            score = MetricScore(
                result_id=result.id,
                metric_code=score_data["metric_code"],
                score=score_data.get("score", 0.0),
                raw_score=score_data.get("raw_score", 0.0),
                passed=score_data.get("passed"),
                details=score_data.get("details"),
                reasoning=score_data.get("reasoning"),
                error=score_data.get("error"),
            )
            db.add(score)

        await db.commit()

    async def _store_failed_result(
        self,
        db: AsyncSession,
        task_id: str,
        test_case_id: str,
        error_message: str,
    ):
        """Store a failed result."""
        result = EvaluationResult(
            task_id=task_id,
            test_case_id=test_case_id,
            status="FAILED",
            error_message=error_message,
        )
        db.add(result)
        await db.commit()

    async def _update_task_status(
        self,
        db: AsyncSession,
        task_id: str,
        status: EvaluationStatus,
        **kwargs,
    ):
        """Update task status."""
        result = await db.execute(
            select(EvaluationTask).where(EvaluationTask.id == task_id)
        )
        task = result.scalar_one()
        task.status = status.value

        for key, value in kwargs.items():
            if hasattr(task, key):
                setattr(task, key, value)

        await db.commit()

    async def _update_task_progress(
        self,
        db: AsyncSession,
        task_id: str,
        completed: int,
        failed: int,
    ):
        """Update task progress."""
        result = await db.execute(
            select(EvaluationTask).where(EvaluationTask.id == task_id)
        )
        task = result.scalar_one()
        task.completed_count = completed
        task.failed_count = failed
        await db.commit()

    async def _finalize_task(
        self,
        db: AsyncSession,
        task_id: str,
        status: EvaluationStatus,
        overall_score: Optional[float],
        metric_summaries: dict,
        total_tokens: int,
        total_cost: float,
        completed_count: int,
        failed_count: int,
    ):
        """Finalize task with results."""
        result = await db.execute(
            select(EvaluationTask).where(EvaluationTask.id == task_id)
        )
        task = result.scalar_one()

        task.status = status.value
        task.overall_score = overall_score
        task.metric_summaries = metric_summaries
        task.total_tokens = total_tokens
        task.total_cost = total_cost
        task.completed_count = completed_count
        task.failed_count = failed_count
        task.completed_at = datetime.utcnow()

        await db.commit()

    async def _send_callback(self, url: str, data: dict):
        """Send callback notification to Spring Boot."""
        try:
            async with httpx.AsyncClient(timeout=30) as client:
                response = await client.post(url, json=data)
                response.raise_for_status()
                logger.info("Callback sent successfully", url=url)
        except Exception as e:
            logger.error("Callback failed", url=url, error=str(e))
