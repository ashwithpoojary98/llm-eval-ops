package io.github.ashwithpoojary98.llmops_eval.evaluation.dto;

import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.EvaluationMode;
import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.EvaluationStatus;
import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.TriggerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRunResponse {

    private String id;
    private String projectId;
    private String projectName;
    private String datasetId;
    private String datasetName;
    private String name;
    private Integer runNumber;
    private EvaluationMode evaluationMode;
    private String targetLlmEndpointId;
    private String targetLlmEndpointName;
    private String judgeLlmEndpointId;
    private String judgeLlmEndpointName;
    private EvaluationStatus status;
    private Integer totalTestCases;
    private Integer completedTestCases;
    private Integer failedTestCases;
    private Double overallScore;
    private Map<String, MetricSummary> metricSummaries;
    private Long totalTokens;
    private BigDecimal totalCost;
    private TriggerType triggerType;
    private String triggerReference;
    private String errorMessage;
    private String createdById;
    private String createdByName;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private List<EvaluationRunMetricResponse> metrics;
}
