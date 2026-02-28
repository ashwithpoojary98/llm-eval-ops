package io.github.ashwithpoojary98.llmops_eval.llms.dto;

import io.github.ashwithpoojary98.llmops_eval.llms.entity.AuthType;
import io.github.ashwithpoojary98.llmops_eval.llms.entity.LlmType;
import io.github.ashwithpoojary98.llmops_eval.llms.entity.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmEndpointResponse {

    private String id;
    private String projectId;
    private String projectName;
    private String name;
    private String description;
    private ProviderType providerType;
    private String modelName;
    private String apiUrl;
    private AuthType authType;
    private Boolean hasApiKey; // Don't expose actual key
    private Map<String, Object> modelConfig;
    private LlmType llmType;
    private Boolean isActive;
    private Boolean isDefault;
    private String createdById;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;
}
