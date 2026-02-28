package io.github.ashwithpoojary98.llmops_eval.llms.dto;

import io.github.ashwithpoojary98.llmops_eval.llms.entity.AuthType;
import io.github.ashwithpoojary98.llmops_eval.llms.entity.LlmType;
import io.github.ashwithpoojary98.llmops_eval.llms.entity.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateLlmEndpointRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "Provider type is required")
    private ProviderType providerType;

    @NotBlank(message = "Model name is required")
    @Size(max = 100)
    private String modelName;

    @Size(max = 500)
    private String apiUrl;

    @Builder.Default
    private AuthType authType = AuthType.API_KEY;

    @NotBlank(message = "API key is required")
    private String apiKey;

    private Map<String, Object> modelConfig;

    @NotNull(message = "LLM type is required")
    @Builder.Default
    private LlmType llmType = LlmType.CLIENT;

    @Builder.Default
    private Boolean isDefault = false;
}
