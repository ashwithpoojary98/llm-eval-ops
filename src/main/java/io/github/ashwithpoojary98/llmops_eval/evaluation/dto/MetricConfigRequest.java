package io.github.ashwithpoojary98.llmops_eval.evaluation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricConfigRequest {

    @NotNull(message = "Metric ID is required")
    private UUID metricId;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "10.0")
    @Builder.Default
    private Double weight = 1.0;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double passThreshold;

    private String customPromptTemplate;
}
