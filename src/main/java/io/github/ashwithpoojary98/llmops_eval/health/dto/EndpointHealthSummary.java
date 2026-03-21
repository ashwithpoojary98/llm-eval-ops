package io.github.ashwithpoojary98.llmops_eval.health.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ashwithpoojary98.llmops_eval.health.entity.HealthStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EndpointHealthSummary {
    private String endpointId;
    private String endpointName;
    private String providerType;
    private String modelName;

    private HealthStatus status;
    private Long latencyMs;
    private Instant lastCheckedAt;
    private String lastErrorMessage;
    private Integer consecutiveFailures;
    private BigDecimal uptimePercentage;
}
