package io.github.ashwithpoojary98.llmops_eval.llms.dto;

import io.github.ashwithpoojary98.llmops_eval.llms.entity.LlmType;
import io.github.ashwithpoojary98.llmops_eval.llms.entity.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmEndpointSummaryResponse {

    private String id;
    private String projectId;
    private String projectName;
    private String name;
    private ProviderType providerType;
    private String modelName;
    private LlmType llmType;
    private Boolean isActive;
    private Boolean isDefault;
    private Instant createdAt;
}
