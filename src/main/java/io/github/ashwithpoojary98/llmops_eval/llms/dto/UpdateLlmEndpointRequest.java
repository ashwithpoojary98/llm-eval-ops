package io.github.ashwithpoojary98.llmops_eval.llms.dto;

import io.github.ashwithpoojary98.llmops_eval.llms.entity.AuthType;
import io.github.ashwithpoojary98.llmops_eval.llms.entity.LlmType;
import io.github.ashwithpoojary98.llmops_eval.llms.entity.ProviderType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLlmEndpointRequest {

    @Size(min = 1, max = 100)
    private String name;

    @Size(max = 2000)
    private String description;

    private ProviderType providerType;

    @Size(max = 100)
    private String modelName;

    @Size(max = 500)
    private String apiUrl;

    private AuthType authType;

    private String apiKey; // Only update if provided

    private Map<String, Object> modelConfig;

    private LlmType llmType;

    private Boolean isDefault;
}
