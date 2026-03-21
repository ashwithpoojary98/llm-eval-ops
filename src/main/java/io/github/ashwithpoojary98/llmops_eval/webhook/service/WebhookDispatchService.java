package io.github.ashwithpoojary98.llmops_eval.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ashwithpoojary98.llmops_eval.llms.service.EncryptionService;
import io.github.ashwithpoojary98.llmops_eval.webhook.entity.DeliveryStatus;
import io.github.ashwithpoojary98.llmops_eval.webhook.entity.Webhook;
import io.github.ashwithpoojary98.llmops_eval.webhook.entity.WebhookDelivery;
import io.github.ashwithpoojary98.llmops_eval.webhook.entity.WebhookEvent;
import io.github.ashwithpoojary98.llmops_eval.webhook.repository.WebhookDeliveryRepository;
import io.github.ashwithpoojary98.llmops_eval.webhook.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookDispatchService {

    private static final int[] RETRY_DELAY_SECONDS = {60, 300}; // 1 min, 5 min

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final EncryptionService encryptionService;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    /**
     * Dispatch an event to all matching webhooks. Called async so it never blocks the caller.
     */
    @Async("eventTaskExecutor")
    public void dispatch(UUID orgId, UUID projectId, WebhookEvent event, Map<String, Object> eventData) {
        List<Webhook> webhooks = projectId != null
                ? webhookRepository.findActiveByOrgAndProjectAndEvent(orgId, projectId, event.name())
                : webhookRepository.findActiveOrgWideByEvent(orgId, event.name());

        if (webhooks.isEmpty()) {
            return;
        }

        Map<String, Object> payload = buildPayload(event, eventData, orgId);

        for (Webhook webhook : webhooks) {
            deliverToWebhook(webhook, event.name(), payload);
        }
    }

    @Transactional
    public void deliverToWebhook(Webhook webhook, String eventType, Map<String, Object> payload) {
        WebhookDelivery delivery = WebhookDelivery.builder()
                .webhook(webhook)
                .eventType(eventType)
                .payload(payload)
                .status(DeliveryStatus.PENDING)
                .triggeredAt(Instant.now())
                .build();

        delivery = deliveryRepository.save(delivery);

        attemptDelivery(webhook, delivery, payload);
    }

    private void attemptDelivery(Webhook webhook, WebhookDelivery delivery, Map<String, Object> payload) {
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);

        try {
            String body = objectMapper.writeValueAsString(payload);
            String signature = computeSignature(webhook, body);

            WebClient client = webClientBuilder
                    .baseUrl(webhook.getUrl())
                    .build();

            var request = client.post()
                    .uri("")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-LLMOps-Event", delivery.getEventType())
                    .header("X-LLMOps-Delivery", delivery.getId().toString());

            if (signature != null) {
                request = request.header("X-LLMOps-Signature-256", "sha256=" + signature);
            }

            String responseBody = request
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            delivery.setStatus(DeliveryStatus.SUCCESS);
            delivery.setResponseStatusCode(200);
            delivery.setResponseBody(responseBody != null ? responseBody.substring(0, Math.min(500, responseBody.length())) : null);
            delivery.setDeliveredAt(Instant.now());
            delivery.setNextRetryAt(null);

            // Reset failure count on success
            webhook.setLastTriggeredAt(Instant.now());
            webhook.setFailureCount(0);

            log.debug("Webhook {} delivered successfully for event {}", webhook.getId(), delivery.getEventType());

        } catch (WebClientResponseException e) {
            handleDeliveryFailure(webhook, delivery, e.getStatusCode().value(), e.getResponseBodyAsString(), e.getMessage());
        } catch (Exception e) {
            handleDeliveryFailure(webhook, delivery, null, null, e.getMessage());
        }

        deliveryRepository.save(delivery);
        webhookRepository.save(webhook);
    }

    private void handleDeliveryFailure(Webhook webhook, WebhookDelivery delivery,
                                        Integer statusCode, String responseBody, String errorMsg) {
        delivery.setResponseStatusCode(statusCode);
        delivery.setResponseBody(responseBody != null ? responseBody.substring(0, Math.min(500, responseBody.length())) : null);
        delivery.setErrorMessage(errorMsg);

        webhook.setFailureCount(webhook.getFailureCount() + 1);

        int attempt = delivery.getAttemptCount();
        if (attempt < delivery.getMaxAttempts()) {
            int delaySeconds = RETRY_DELAY_SECONDS[Math.min(attempt - 1, RETRY_DELAY_SECONDS.length - 1)];
            delivery.setStatus(DeliveryStatus.RETRYING);
            delivery.setNextRetryAt(Instant.now().plusSeconds(delaySeconds));
            log.warn("Webhook {} delivery failed (attempt {}), retry in {}s", webhook.getId(), attempt, delaySeconds);
        } else {
            delivery.setStatus(DeliveryStatus.FAILED);
            log.error("Webhook {} delivery failed after {} attempts, giving up", webhook.getId(), attempt);

            // Auto-disable after too many consecutive failures
            if (webhook.getFailureCount() >= 10) {
                webhook.setIsActive(false);
                log.warn("Webhook {} auto-disabled after 10 consecutive failures", webhook.getId());
            }
        }
    }

    /**
     * Retry scheduler — picks up RETRYING deliveries whose retry time has passed.
     */
    @Scheduled(fixedDelay = 60000) // every minute
    @Transactional
    public void retryFailedDeliveries() {
        List<WebhookDelivery> pending = deliveryRepository.findPendingRetries(Instant.now());
        if (!pending.isEmpty()) {
            log.debug("Retrying {} failed webhook deliveries", pending.size());
        }
        for (WebhookDelivery delivery : pending) {
            attemptDelivery(delivery.getWebhook(), delivery, delivery.getPayload());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<String, Object> buildPayload(WebhookEvent event, Map<String, Object> data, UUID orgId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        payload.put("event", event.name());
        payload.put("timestamp", Instant.now().toString());
        payload.put("organization", orgId.toString());
        payload.put("data", data);
        return payload;
    }

    private String computeSignature(Webhook webhook, String body) {
        if (webhook.getSecretEncrypted() == null || webhook.getSecretEncrypted().isBlank()) {
            return null;
        }
        try {
            String secret = encryptionService.decrypt(webhook.getSecretEncrypted());
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Failed to compute HMAC signature for webhook {}: {}", webhook.getId(), e.getMessage());
            return null;
        }
    }
}
