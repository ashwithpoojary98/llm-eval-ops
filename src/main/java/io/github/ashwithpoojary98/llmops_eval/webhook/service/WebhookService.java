package io.github.ashwithpoojary98.llmops_eval.webhook.service;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.Organization;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.ForbiddenException;
import io.github.ashwithpoojary98.llmops_eval.llms.service.EncryptionService;
import io.github.ashwithpoojary98.llmops_eval.project.entity.Project;
import io.github.ashwithpoojary98.llmops_eval.project.repository.ProjectRepository;
import io.github.ashwithpoojary98.llmops_eval.project.service.ProjectAccessService;
import io.github.ashwithpoojary98.llmops_eval.webhook.dto.CreateWebhookRequest;
import io.github.ashwithpoojary98.llmops_eval.webhook.dto.UpdateWebhookRequest;
import io.github.ashwithpoojary98.llmops_eval.webhook.dto.WebhookDeliveryResponse;
import io.github.ashwithpoojary98.llmops_eval.webhook.dto.WebhookResponse;
import io.github.ashwithpoojary98.llmops_eval.webhook.entity.Webhook;
import io.github.ashwithpoojary98.llmops_eval.webhook.entity.WebhookDelivery;
import io.github.ashwithpoojary98.llmops_eval.webhook.entity.WebhookEvent;
import io.github.ashwithpoojary98.llmops_eval.webhook.exception.WebhookNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.webhook.repository.WebhookDeliveryRepository;
import io.github.ashwithpoojary98.llmops_eval.webhook.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService projectAccessService;
    private final EncryptionService encryptionService;
    private final WebhookDispatchService dispatchService;

    @Transactional
    public WebhookResponse createWebhook(CreateWebhookRequest request, User user) {
        Organization org = user.getOrganization();

        // Validate event names
        validateEvents(request.getEvents());

        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.getProjectId()));
            projectAccessService.getUserAccessLevel(project.getId(), user)
                    .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));
        }

        String encryptedSecret = null;
        if (request.getSecret() != null && !request.getSecret().isBlank()) {
            encryptedSecret = encryptionService.encrypt(request.getSecret());
        }

        Webhook webhook = Webhook.builder()
                .organization(org)
                .project(project)
                .name(request.getName())
                .url(request.getUrl())
                .secretEncrypted(encryptedSecret)
                .events(request.getEvents())
                .createdBy(user)
                .build();

        webhook = webhookRepository.save(webhook);
        log.info("Created webhook {} for org {}", webhook.getId(), org.getId());

        return toResponse(webhook);
    }

    @Transactional(readOnly = true)
    public Page<WebhookResponse> listWebhooks(UUID projectId, User user, Pageable pageable) {
        UUID orgId = user.getOrganization().getId();
        Page<Webhook> webhooks;

        if (projectId != null) {
            projectAccessService.getUserAccessLevel(projectId, user)
                    .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));
            webhooks = webhookRepository.findByOrganizationIdAndProjectId(orgId, projectId, pageable);
        } else {
            webhooks = webhookRepository.findByOrganizationId(orgId, pageable);
        }

        return webhooks.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public WebhookResponse getWebhook(UUID webhookId, User user) {
        Webhook webhook = findAndAuthorize(webhookId, user);
        return toResponse(webhook);
    }

    @Transactional
    public WebhookResponse updateWebhook(UUID webhookId, UpdateWebhookRequest request, User user) {
        Webhook webhook = findAndAuthorize(webhookId, user);

        if (request.getName() != null) webhook.setName(request.getName());
        if (request.getUrl() != null) webhook.setUrl(request.getUrl());
        if (request.getSecret() != null && !request.getSecret().isBlank()) {
            webhook.setSecretEncrypted(encryptionService.encrypt(request.getSecret()));
        }
        if (request.getEvents() != null) {
            validateEvents(request.getEvents());
            webhook.setEvents(request.getEvents());
        }
        if (request.getIsActive() != null) webhook.setIsActive(request.getIsActive());

        webhook = webhookRepository.save(webhook);
        log.info("Updated webhook {}", webhookId);

        return toResponse(webhook);
    }

    @Transactional
    public void deleteWebhook(UUID webhookId, User user) {
        Webhook webhook = findAndAuthorize(webhookId, user);
        webhookRepository.delete(webhook);
        log.info("Deleted webhook {}", webhookId);
    }

    @Transactional(readOnly = true)
    public Page<WebhookDeliveryResponse> getDeliveries(UUID webhookId, User user, Pageable pageable) {
        findAndAuthorize(webhookId, user);
        return deliveryRepository.findByWebhookIdOrderByTriggeredAtDesc(webhookId, pageable)
                .map(this::toDeliveryResponse);
    }

    /**
     * Send a test ping payload to the webhook URL.
     */
    @Transactional
    public WebhookDeliveryResponse sendTestEvent(UUID webhookId, User user) {
        Webhook webhook = findAndAuthorize(webhookId, user);

        Map<String, Object> testData = Map.of(
                "message", "This is a test delivery from LLMOps Eval",
                "webhookId", webhookId.toString(),
                "triggeredBy", user.getEmail()
        );

        dispatchService.deliverToWebhook(webhook, "TEST_EVENT", Map.of(
                "id", "evt_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
                "event", "TEST_EVENT",
                "timestamp", java.time.Instant.now().toString(),
                "organization", webhook.getOrganization().getId().toString(),
                "data", testData
        ));

        // Return the latest delivery record
        return deliveryRepository
                .findByWebhookIdOrderByTriggeredAtDesc(webhookId, Pageable.ofSize(1))
                .stream()
                .findFirst()
                .map(this::toDeliveryResponse)
                .orElseThrow();
    }

    /**
     * Return all supported event types.
     */
    public List<String> listSupportedEvents() {
        return Arrays.stream(WebhookEvent.values())
                .map(WebhookEvent::name)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Webhook findAndAuthorize(UUID webhookId, User user) {
        Webhook webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new WebhookNotFoundException("Webhook not found: " + webhookId));

        if (!webhook.getOrganization().getId().equals(user.getOrganization().getId())) {
            throw new ForbiddenException("You don't have access to this webhook");
        }

        return webhook;
    }

    private void validateEvents(List<String> events) {
        List<String> valid = Arrays.stream(WebhookEvent.values()).map(WebhookEvent::name).toList();
        List<String> invalid = events.stream().filter(e -> !valid.contains(e)).toList();
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("Unknown event types: " + invalid + ". Valid events: " + valid);
        }
    }

    private WebhookResponse toResponse(Webhook w) {
        return WebhookResponse.builder()
                .id(w.getId().toString())
                .organizationId(w.getOrganization().getId().toString())
                .projectId(w.getProject() != null ? w.getProject().getId().toString() : null)
                .projectName(w.getProject() != null ? w.getProject().getName() : null)
                .name(w.getName())
                .url(w.getUrl())
                .hasSecret(w.getSecretEncrypted() != null && !w.getSecretEncrypted().isBlank())
                .events(w.getEvents())
                .isActive(w.getIsActive())
                .failureCount(w.getFailureCount())
                .lastTriggeredAt(w.getLastTriggeredAt())
                .createdById(w.getCreatedBy().getId().toString())
                .createdByName(w.getCreatedBy().getFullName())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }

    private WebhookDeliveryResponse toDeliveryResponse(WebhookDelivery d) {
        return WebhookDeliveryResponse.builder()
                .id(d.getId().toString())
                .webhookId(d.getWebhook().getId().toString())
                .eventType(d.getEventType())
                .payload(d.getPayload())
                .status(d.getStatus())
                .responseStatusCode(d.getResponseStatusCode())
                .responseBody(d.getResponseBody())
                .errorMessage(d.getErrorMessage())
                .attemptCount(d.getAttemptCount())
                .maxAttempts(d.getMaxAttempts())
                .nextRetryAt(d.getNextRetryAt())
                .triggeredAt(d.getTriggeredAt())
                .deliveredAt(d.getDeliveredAt())
                .build();
    }
}
