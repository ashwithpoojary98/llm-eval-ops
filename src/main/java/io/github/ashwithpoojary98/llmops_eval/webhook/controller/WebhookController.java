package io.github.ashwithpoojary98.llmops_eval.webhook.controller;

import io.github.ashwithpoojary98.llmops_eval.auth.model.APIResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.security.AuthenticatedUser;
import io.github.ashwithpoojary98.llmops_eval.auth.service.UserService;
import io.github.ashwithpoojary98.llmops_eval.webhook.dto.CreateWebhookRequest;
import io.github.ashwithpoojary98.llmops_eval.webhook.dto.UpdateWebhookRequest;
import io.github.ashwithpoojary98.llmops_eval.webhook.dto.WebhookDeliveryResponse;
import io.github.ashwithpoojary98.llmops_eval.webhook.dto.WebhookResponse;
import io.github.ashwithpoojary98.llmops_eval.webhook.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;
    private final UserService userService;

    /**
     * GET /api/webhooks/events
     * List all supported event types.
     */
    @GetMapping("/events")
    public ResponseEntity<APIResponse<List<String>>> listSupportedEvents() {
        return ResponseEntity.ok(APIResponse.<List<String>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Supported webhook events")
                .data(webhookService.listSupportedEvents())
                .timestamp(Instant.now())
                .build());
    }

    /**
     * POST /api/webhooks
     * Register a new webhook.
     */
    @PostMapping
    public ResponseEntity<APIResponse<WebhookResponse>> createWebhook(
            @Valid @RequestBody CreateWebhookRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        WebhookResponse response = webhookService.createWebhook(request, user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<WebhookResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Webhook registered successfully")
                        .data(response)
                        .timestamp(Instant.now())
                        .build());
    }

    /**
     * GET /api/webhooks
     * List webhooks for the user's organization. Optional ?projectId= to filter.
     */
    @GetMapping
    public ResponseEntity<APIResponse<Page<WebhookResponse>>> listWebhooks(
            @RequestParam(required = false) UUID projectId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        Page<WebhookResponse> response = webhookService.listWebhooks(projectId, user, pageable);

        return ResponseEntity.ok(APIResponse.<Page<WebhookResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Webhooks retrieved successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    /**
     * GET /api/webhooks/{webhookId}
     */
    @GetMapping("/{webhookId}")
    public ResponseEntity<APIResponse<WebhookResponse>> getWebhook(
            @PathVariable UUID webhookId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        WebhookResponse response = webhookService.getWebhook(webhookId, user);

        return ResponseEntity.ok(APIResponse.<WebhookResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Webhook retrieved successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    /**
     * PUT /api/webhooks/{webhookId}
     */
    @PutMapping("/{webhookId}")
    public ResponseEntity<APIResponse<WebhookResponse>> updateWebhook(
            @PathVariable UUID webhookId,
            @Valid @RequestBody UpdateWebhookRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        WebhookResponse response = webhookService.updateWebhook(webhookId, request, user);

        return ResponseEntity.ok(APIResponse.<WebhookResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Webhook updated successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    /**
     * DELETE /api/webhooks/{webhookId}
     */
    @DeleteMapping("/{webhookId}")
    public ResponseEntity<APIResponse<Void>> deleteWebhook(
            @PathVariable UUID webhookId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        webhookService.deleteWebhook(webhookId, user);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Webhook deleted successfully")
                .timestamp(Instant.now())
                .build());
    }

    /**
     * GET /api/webhooks/{webhookId}/deliveries
     * Audit log of all deliveries for a webhook.
     */
    @GetMapping("/{webhookId}/deliveries")
    public ResponseEntity<APIResponse<Page<WebhookDeliveryResponse>>> getDeliveries(
            @PathVariable UUID webhookId,
            @PageableDefault(size = 30, sort = "triggeredAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        Page<WebhookDeliveryResponse> response = webhookService.getDeliveries(webhookId, user, pageable);

        return ResponseEntity.ok(APIResponse.<Page<WebhookDeliveryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Webhook deliveries retrieved")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    /**
     * POST /api/webhooks/{webhookId}/test
     * Fire a test event to verify the webhook URL works.
     */
    @PostMapping("/{webhookId}/test")
    public ResponseEntity<APIResponse<WebhookDeliveryResponse>> testWebhook(
            @PathVariable UUID webhookId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        WebhookDeliveryResponse response = webhookService.sendTestEvent(webhookId, user);

        return ResponseEntity.ok(APIResponse.<WebhookDeliveryResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Test event sent")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }
}
