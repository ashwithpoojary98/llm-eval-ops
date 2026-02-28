package io.github.ashwithpoojary98.llmops_eval.evaluation.repository;

import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.EvaluationRun;
import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.EvaluationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvaluationRunRepository extends JpaRepository<EvaluationRun, UUID> {

    Page<EvaluationRun> findByProjectId(UUID projectId, Pageable pageable);

    Page<EvaluationRun> findByProjectIdAndStatus(UUID projectId, EvaluationStatus status, Pageable pageable);

    Page<EvaluationRun> findByDatasetId(UUID datasetId, Pageable pageable);

    @Query("SELECT e FROM EvaluationRun e " +
           "LEFT JOIN FETCH e.project " +
           "LEFT JOIN FETCH e.dataset " +
           "LEFT JOIN FETCH e.createdBy " +
           "LEFT JOIN FETCH e.targetLlmEndpoint " +
           "LEFT JOIN FETCH e.judgeLlmEndpoint " +
           "WHERE e.id = :id")
    Optional<EvaluationRun> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT e FROM EvaluationRun e " +
           "LEFT JOIN FETCH e.runMetrics rm " +
           "LEFT JOIN FETCH rm.metric " +
           "WHERE e.id = :id")
    Optional<EvaluationRun> findByIdWithMetrics(@Param("id") UUID id);

    /**
     * Get the next run number for a project + name combination.
     */
    @Query("SELECT COALESCE(MAX(e.runNumber), 0) + 1 FROM EvaluationRun e " +
           "WHERE e.project.id = :projectId AND e.name = :name")
    Integer getNextRunNumber(@Param("projectId") UUID projectId, @Param("name") String name);

    /**
     * Find runs in progress (for monitoring).
     */
    List<EvaluationRun> findByStatusIn(List<EvaluationStatus> statuses);

    /**
     * Find stale running evaluations (for cleanup).
     */
    @Query("SELECT e FROM EvaluationRun e " +
           "WHERE e.status = 'RUNNING' " +
           "AND e.startedAt < :cutoff")
    List<EvaluationRun> findStaleRunningEvaluations(@Param("cutoff") Instant cutoff);

    /**
     * Update evaluation status.
     */
    @Modifying
    @Query("UPDATE EvaluationRun e SET e.status = :status, e.completedAt = :completedAt " +
           "WHERE e.id = :id")
    void updateStatus(
            @Param("id") UUID id,
            @Param("status") EvaluationStatus status,
            @Param("completedAt") Instant completedAt);

    /**
     * Update evaluation progress.
     */
    @Modifying
    @Query("UPDATE EvaluationRun e " +
           "SET e.completedTestCases = :completed, e.failedTestCases = :failed " +
           "WHERE e.id = :id")
    void updateProgress(
            @Param("id") UUID id,
            @Param("completed") Integer completed,
            @Param("failed") Integer failed);

    /**
     * Search evaluations.
     */
    @Query("""
            SELECT e FROM EvaluationRun e
            WHERE e.project.id = :projectId
            AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(e.dataset.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<EvaluationRun> searchByProjectId(
            @Param("projectId") UUID projectId,
            @Param("search") String search,
            Pageable pageable);

    long countByProjectId(UUID projectId);

    long countByProjectIdAndStatus(UUID projectId, EvaluationStatus status);
}
