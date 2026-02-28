package io.github.ashwithpoojary98.llmops_eval.evaluation.dto;

import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.ResultStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResultResponse {

    private String id;
    private String testCaseId;
    private String question;
    private String groundTruth;
    private String llmOutput;
    private String generatedOutput;
    private ResultStatus status;
    private Integer llmLatencyMs;
    private Integer processingTimeMs;
    private Integer inputTokens;
    private Integer outputTokens;
    private String errorMessage;
    private Instant createdAt;
    private List<EvaluationScoreResponse> scores;
}
