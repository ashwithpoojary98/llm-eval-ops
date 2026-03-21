package io.github.ashwithpoojary98.llmops_eval.settings.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class PlatformSettingsRequest {
    private String baseUrl;
    private String platformName;

    @Email(message = "Support email must be a valid email address")
    private String supportEmail;
}
