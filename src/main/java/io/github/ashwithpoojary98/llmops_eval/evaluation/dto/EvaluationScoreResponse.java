package io.github.ashwithpoojary98.llmops_eval.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationScoreResponse {

    private String metricCode;
    private String metricName;
    private Double score;
    private Double rawScore;
    private Boolean passed;
    private Map<String, Object> details;
    private String judgeReasoning;
    private String errorMessage;
}
