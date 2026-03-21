package io.github.ashwithpoojary98.llmops_eval.health.entity;

import io.github.ashwithpoojary98.llmops_eval.llms.entity.LlmEndpoint;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "llm_endpoint_health_status")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmEndpointHealthStatus {

    @Id
    @Column(name = "endpoint_id")
    private UUID endpointId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "endpoint_id")
    private LlmEndpoint endpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private HealthStatus status = HealthStatus.UNKNOWN;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "consecutive_failures", nullable = false)
    @Builder.Default
    private Integer consecutiveFailures = 0;

    @Column(name = "uptime_percentage", precision = 5, scale = 2)
    private BigDecimal uptimePercentage;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
