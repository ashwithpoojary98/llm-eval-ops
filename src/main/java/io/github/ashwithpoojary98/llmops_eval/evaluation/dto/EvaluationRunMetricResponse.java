package io.github.ashwithpoojary98.llmops_eval.evaluation.dto;

import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.MetricCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRunMetricResponse {

    private String metricId;
    private String metricCode;
    private String metricName;
    private MetricCategory category;
    private Double weight;
    private Double passThreshold;
    private Boolean hasCustomPrompt;
}
