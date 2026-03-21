package io.github.ashwithpoojary98.llmops_eval.health.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ashwithpoojary98.llmops_eval.health.entity.CheckType;
import io.github.ashwithpoojary98.llmops_eval.health.entity.HealthStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LlmHealthResponse {
    private String recordId;
    private String endpointId;
    private String endpointName;
    private String providerType;
    private String modelName;

    // Health result
    private HealthStatus status;
    private Long latencyMs;
    private Integer httpStatusCode;
    private String errorMessage;

    // Model details
    private Integer contextWindow;
    private Integer maxOutputTokens;

    // Meta
    private CheckType checkType;
    private Instant checkedAt;
}
