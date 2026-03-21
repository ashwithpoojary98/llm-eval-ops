package io.github.ashwithpoojary98.llmops_eval.settings.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlatformSettingsResponse {
    private String baseUrl;
    private String platformName;
    private String supportEmail;
}
