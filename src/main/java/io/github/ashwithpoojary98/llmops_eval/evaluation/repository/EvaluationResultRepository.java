package io.github.ashwithpoojary98.llmops_eval.evaluation.repository;

import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.EvaluationResult;
import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.ResultStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, UUID> {

    Page<EvaluationResult> findByEvaluationRunId(UUID evaluationRunId, Pageable pageable);

    List<EvaluationResult> findByEvaluationRunId(UUID evaluationRunId);

    void deleteByEvaluationRunId(UUID evaluationRunId);

    @Query("SELECT r FROM EvaluationResult r " +
           "LEFT JOIN FETCH r.testCase " +
           "LEFT JOIN FETCH r.scores s " +
           "LEFT JOIN FETCH s.metric " +
           "WHERE r.id = :id")
    Optional<EvaluationResult> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT r FROM EvaluationResult r " +
           "LEFT JOIN FETCH r.testCase " +
           "LEFT JOIN FETCH r.scores s " +
           "LEFT JOIN FETCH s.metric " +
           "WHERE r.evaluationRun.id = :runId")
    List<EvaluationResult> findByEvaluationRunIdWithDetails(@Param("runId") UUID runId);

    long countByEvaluationRunIdAndStatus(UUID evaluationRunId, ResultStatus status);

    /**
     * Find results with failed status for retry.
     */
    List<EvaluationResult> findByEvaluationRunIdAndStatus(UUID evaluationRunId, ResultStatus status);

    /**
     * Get average score for a metric across all results in a run.
     */
    @Query("""
            SELECT AVG(s.score) FROM EvaluationScore s
            WHERE s.evaluationResult.evaluationRun.id = :runId
            AND s.metric.id = :metricId
            AND s.errorMessage IS NULL
            """)
    Double getAverageScoreForMetric(
            @Param("runId") UUID runId,
            @Param("metricId") UUID metricId);
}
