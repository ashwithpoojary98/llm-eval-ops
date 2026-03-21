package io.github.ashwithpoojary98.llmops_eval.health.repository;

import io.github.ashwithpoojary98.llmops_eval.health.entity.LlmEndpointHealthStatus;
import io.github.ashwithpoojary98.llmops_eval.health.entity.HealthStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LlmEndpointHealthStatusRepository extends JpaRepository<LlmEndpointHealthStatus, UUID> {

    @Query("SELECT s FROM LlmEndpointHealthStatus s JOIN s.endpoint e JOIN e.project p WHERE p.organization.id = :orgId")
    List<LlmEndpointHealthStatus> findAllByOrganizationId(@Param("orgId") UUID orgId);

    @Query("SELECT s FROM LlmEndpointHealthStatus s JOIN s.endpoint e WHERE e.project.id = :projectId")
    List<LlmEndpointHealthStatus> findAllByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT COUNT(s) FROM LlmEndpointHealthStatus s JOIN s.endpoint e WHERE e.project.id = :projectId AND s.status = :status")
    long countByProjectIdAndStatus(@Param("projectId") UUID projectId, @Param("status") HealthStatus status);
}
