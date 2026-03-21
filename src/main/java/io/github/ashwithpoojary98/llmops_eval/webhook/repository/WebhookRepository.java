package io.github.ashwithpoojary98.llmops_eval.webhook.repository;

import io.github.ashwithpoojary98.llmops_eval.webhook.entity.Webhook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WebhookRepository extends JpaRepository<Webhook, UUID> {

    Page<Webhook> findByOrganizationId(UUID organizationId, Pageable pageable);

    Page<Webhook> findByOrganizationIdAndProjectId(UUID organizationId, UUID projectId, Pageable pageable);

    /**
     * Find all active webhooks for an org+project that subscribe to a given event.
     * Checks both org-wide (project_id IS NULL) and project-scoped webhooks.
     */
    @Query("""
        SELECT w FROM Webhook w
        WHERE w.isActive = true
          AND w.organization.id = :orgId
          AND (w.project IS NULL OR w.project.id = :projectId)
          AND CAST(w.events AS string) LIKE %:event%
    """)
    List<Webhook> findActiveByOrgAndProjectAndEvent(
            @Param("orgId") UUID orgId,
            @Param("projectId") UUID projectId,
            @Param("event") String event);

    /**
     * Find active org-wide webhooks subscribed to an event.
     */
    @Query("""
        SELECT w FROM Webhook w
        WHERE w.isActive = true
          AND w.organization.id = :orgId
          AND w.project IS NULL
          AND CAST(w.events AS string) LIKE %:event%
    """)
    List<Webhook> findActiveOrgWideByEvent(
            @Param("orgId") UUID orgId,
            @Param("event") String event);
}
