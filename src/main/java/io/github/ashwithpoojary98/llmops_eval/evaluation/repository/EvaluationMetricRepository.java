package io.github.ashwithpoojary98.llmops_eval.evaluation.repository;

import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.EvaluationMetric;
import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.MetricCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvaluationMetricRepository extends JpaRepository<EvaluationMetric, UUID> {

    /**
     * Find all system-defined metrics.
     */
    List<EvaluationMetric> findByIsSystemTrueAndIsActiveTrue();

    /**
     * Find metrics available for a project (system + project-specific).
     */
    @Query("""
            SELECT m FROM EvaluationMetric m
            WHERE m.isActive = true
            AND (m.isSystem = true OR m.project.id = :projectId)
            ORDER BY m.category, m.displayName
            """)
    List<EvaluationMetric> findAvailableMetricsForProject(@Param("projectId") UUID projectId);

    /**
     * Find metric by code (system-level).
     */
    Optional<EvaluationMetric> findByCodeAndIsSystemTrue(String code);

    /**
     * Find metric by code for a specific project.
     */
    @Query("""
            SELECT m FROM EvaluationMetric m
            WHERE m.code = :code
            AND m.isActive = true
            AND (m.isSystem = true OR m.project.id = :projectId)
            """)
    Optional<EvaluationMetric> findByCodeForProject(
            @Param("code") String code,
            @Param("projectId") UUID projectId);

    /**
     * Find metrics by category.
     */
    List<EvaluationMetric> findByCategoryAndIsActiveTrue(MetricCategory category);

    /**
     * Find metrics that require judge LLM.
     */
    @Query("""
            SELECT m FROM EvaluationMetric m
            WHERE m.requiresJudgeLlm = true
            AND m.isActive = true
            AND (m.isSystem = true OR m.project.id = :projectId)
            """)
    List<EvaluationMetric> findJudgeMetricsForProject(@Param("projectId") UUID projectId);

    /**
     * Check if project has custom metrics.
     */
    boolean existsByProjectIdAndIsSystemFalse(UUID projectId);
}
