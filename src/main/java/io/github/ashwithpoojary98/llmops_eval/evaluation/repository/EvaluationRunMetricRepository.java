package io.github.ashwithpoojary98.llmops_eval.evaluation.repository;

import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.EvaluationRunMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationRunMetricRepository extends JpaRepository<EvaluationRunMetric, UUID> {

    List<EvaluationRunMetric> findByEvaluationRunId(UUID evaluationRunId);

    @Query("""
            SELECT rm FROM EvaluationRunMetric rm
            LEFT JOIN FETCH rm.metric
            WHERE rm.evaluationRun.id = :runId
            """)
    List<EvaluationRunMetric> findByEvaluationRunIdWithMetric(@Param("runId") UUID runId);

    void deleteByEvaluationRunId(UUID evaluationRunId);
}
