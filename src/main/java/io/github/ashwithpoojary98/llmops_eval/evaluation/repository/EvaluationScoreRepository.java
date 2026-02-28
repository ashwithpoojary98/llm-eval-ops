package io.github.ashwithpoojary98.llmops_eval.evaluation.repository;

import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.EvaluationScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationScoreRepository extends JpaRepository<EvaluationScore, UUID> {

    List<EvaluationScore> findByEvaluationResultId(UUID evaluationResultId);

    @Query("""
            SELECT s FROM EvaluationScore s
            LEFT JOIN FETCH s.metric
            WHERE s.evaluationResult.id = :resultId
            """)
    List<EvaluationScore> findByEvaluationResultIdWithMetric(@Param("resultId") UUID resultId);

    /**
     * Get all scores for a metric in a run.
     */
    @Query("""
            SELECT s FROM EvaluationScore s
            WHERE s.evaluationResult.evaluationRun.id = :runId
            AND s.metric.id = :metricId
            """)
    List<EvaluationScore> findByRunIdAndMetricId(
            @Param("runId") UUID runId,
            @Param("metricId") UUID metricId);

    /**
     * Count passed scores for a metric in a run.
     */
    @Query("""
            SELECT COUNT(s) FROM EvaluationScore s
            WHERE s.evaluationResult.evaluationRun.id = :runId
            AND s.metric.id = :metricId
            AND s.passed = true
            """)
    long countPassedByRunIdAndMetricId(
            @Param("runId") UUID runId,
            @Param("metricId") UUID metricId);
}
