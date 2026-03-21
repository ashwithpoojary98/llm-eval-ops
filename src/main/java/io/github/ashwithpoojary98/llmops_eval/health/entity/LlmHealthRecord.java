package io.github.ashwithpoojary98.llmops_eval.health.entity;

import io.github.ashwithpoojary98.llmops_eval.llms.entity.LlmEndpoint;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "llm_health_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmHealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private LlmEndpoint endpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HealthStatus status;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "provider_type", length = 50)
    private String providerType;

    @Column(name = "context_window")
    private Integer contextWindow;

    @Column(name = "max_output_tokens")
    private Integer maxOutputTokens;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_type", nullable = false, length = 20)
    @Builder.Default
    private CheckType checkType = CheckType.SCHEDULED;

    @Column(name = "checked_at", nullable = false)
    @Builder.Default
    private Instant checkedAt = Instant.now();
}
