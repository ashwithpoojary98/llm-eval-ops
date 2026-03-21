package io.github.ashwithpoojary98.llmops_eval.webhook.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ashwithpoojary98.llmops_eval.webhook.entity.DeliveryStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebhookDeliveryResponse {
    private String id;
    private String webhookId;
    private String eventType;
    private Map<String, Object> payload;
    private DeliveryStatus status;
    private Integer responseStatusCode;
    private String responseBody;
    private String errorMessage;
    private int attemptCount;
    private int maxAttempts;
    private Instant nextRetryAt;
    private Instant triggeredAt;
    private Instant deliveredAt;
}
