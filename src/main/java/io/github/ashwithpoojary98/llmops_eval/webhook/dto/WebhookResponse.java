package io.github.ashwithpoojary98.llmops_eval.webhook.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebhookResponse {
    private String id;
    private String organizationId;
    private String projectId;
    private String projectName;
    private String name;
    private String url;
    private boolean hasSecret;
    private List<String> events;
    private boolean isActive;
    private int failureCount;
    private Instant lastTriggeredAt;
    private String createdById;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;
}
