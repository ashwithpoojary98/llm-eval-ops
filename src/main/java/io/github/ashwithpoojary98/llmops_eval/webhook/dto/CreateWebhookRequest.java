package io.github.ashwithpoojary98.llmops_eval.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.util.List;
import java.util.UUID;

@Data
public class CreateWebhookRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @URL
    private String url;

    /** HMAC signing secret — optional but strongly recommended */
    private String secret;

    @NotEmpty
    private List<String> events;

    /** Scope to a specific project; null = org-wide */
    private UUID projectId;
}
