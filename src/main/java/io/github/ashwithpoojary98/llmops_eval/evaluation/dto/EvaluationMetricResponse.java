package io.github.ashwithpoojary98.llmops_eval.evaluation.dto;

import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.MetricCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationMetricResponse {

    private String id;
    private String code;
    private String displayName;
    private String description;
    private MetricCategory category;
    private Boolean requiresGroundTruth;
    private Boolean requiresContext;
    private Boolean requiresJudgeLlm;
    private Double defaultWeight;
    private Double defaultThreshold;
    private Boolean isSystem;
    private Boolean isActive;
    private Instant createdAt;
}
