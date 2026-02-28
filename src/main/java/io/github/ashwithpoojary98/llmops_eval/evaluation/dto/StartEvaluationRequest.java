package io.github.ashwithpoojary98.llmops_eval.evaluation.dto;

import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.EvaluationMode;
import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.TriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartEvaluationRequest {

    @NotBlank(message = "Evaluation name is required")
    @Size(min = 1, max = 200)
    private String name;

    @NotNull(message = "Dataset ID is required")
    private UUID datasetId;

    @Builder.Default
    private EvaluationMode evaluationMode = EvaluationMode.PRE_GENERATED;

    private UUID targetLlmEndpointId; // Required for LIVE_GENERATION

    private UUID judgeLlmEndpointId;  // Required for LLM-as-Judge metrics

    @NotEmpty(message = "At least one metric is required")
    private List<MetricConfigRequest> metrics;

    @Builder.Default
    private TriggerType triggerType = TriggerType.MANUAL;

    private String triggerReference;
}
