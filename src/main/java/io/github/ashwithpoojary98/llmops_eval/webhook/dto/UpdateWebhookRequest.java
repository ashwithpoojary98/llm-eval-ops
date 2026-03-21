package io.github.ashwithpoojary98.llmops_eval.webhook.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.util.List;

@Data
public class UpdateWebhookRequest {

    @Size(max = 100)
    private String name;

    @URL
    private String url;

    private String secret;

    private List<String> events;

    private Boolean isActive;
}
