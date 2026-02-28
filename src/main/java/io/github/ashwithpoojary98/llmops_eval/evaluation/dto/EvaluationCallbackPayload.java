package io.github.ashwithpoojary98.llmops_eval.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload received from evaluation engine when evaluation completes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationCallbackPayload {

    private String evaluationRunId;
    private String status; // COMPLETED, FAILED
    private Double overallScore;
    private Integer completed;
    private Integer failed;
    private String error;
}
