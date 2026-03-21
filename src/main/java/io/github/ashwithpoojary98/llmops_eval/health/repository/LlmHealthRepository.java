package io.github.ashwithpoojary98.llmops_eval.health.repository;

import io.github.ashwithpoojary98.llmops_eval.health.entity.LlmHealthRecord;
import io.github.ashwithpoojary98.llmops_eval.health.entity.HealthStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LlmHealthRepository extends JpaRepository<LlmHealthRecord, UUID> {

    Page<LlmHealthRecord> findByEndpointIdOrderByCheckedAtDesc(UUID endpointId, Pageable pageable);

    Optional<LlmHealthRecord> findTopByEndpointIdOrderByCheckedAtDesc(UUID endpointId);

    @Query("SELECT r FROM LlmHealthRecord r WHERE r.endpoint.id = :endpointId AND r.checkedAt >= :since ORDER BY r.checkedAt DESC")
    List<LlmHealthRecord> findByEndpointIdSince(@Param("endpointId") UUID endpointId, @Param("since") Instant since);

    @Query("SELECT COUNT(r) FROM LlmHealthRecord r WHERE r.endpoint.id = :endpointId AND r.checkedAt >= :since AND r.status = :status")
    long countByEndpointIdSinceAndStatus(
            @Param("endpointId") UUID endpointId,
            @Param("since") Instant since,
            @Param("status") HealthStatus status);

    @Query("SELECT COUNT(r) FROM LlmHealthRecord r WHERE r.endpoint.id = :endpointId AND r.checkedAt >= :since")
    long countByEndpointIdSince(@Param("endpointId") UUID endpointId, @Param("since") Instant since);

    void deleteByCheckedAtBefore(Instant cutoff);
}
