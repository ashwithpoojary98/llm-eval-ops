package io.github.ashwithpoojary98.llmops_eval.evaluation.dto;

import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.EvaluationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRunSummaryResponse {

    private String id;
    private String name;
    private Integer runNumber;
    private String datasetName;
    private EvaluationStatus status;
    private Integer totalTestCases;
    private Integer completedTestCases;
    private Double overallScore;
    private Instant createdAt;
    private Instant completedAt;
}
