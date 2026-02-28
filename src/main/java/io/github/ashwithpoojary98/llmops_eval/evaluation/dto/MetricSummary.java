package io.github.ashwithpoojary98.llmops_eval.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricSummary {

    private String metricCode;
    private String metricName;
    private Double averageScore;
    private Double minScore;
    private Double maxScore;
    private Double stdDev;
    private Double passRate;
    private Integer totalEvaluated;
}
