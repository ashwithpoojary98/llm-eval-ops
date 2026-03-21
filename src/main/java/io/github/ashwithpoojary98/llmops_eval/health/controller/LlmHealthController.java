package io.github.ashwithpoojary98.llmops_eval.health.controller;

import io.github.ashwithpoojary98.llmops_eval.auth.model.APIResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.security.AuthenticatedUser;
import io.github.ashwithpoojary98.llmops_eval.auth.service.UserService;
import io.github.ashwithpoojary98.llmops_eval.health.dto.HealthDashboardResponse;
import io.github.ashwithpoojary98.llmops_eval.health.dto.LlmHealthResponse;
import io.github.ashwithpoojary98.llmops_eval.health.service.LlmHealthService;
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
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LlmHealthController {

    private final LlmHealthService llmHealthService;
    private final UserService userService;

    /**
     * POST /api/llm-endpoints/{endpointId}/health/check
     * Trigger a manual health check on demand.
     */
    @PostMapping("/api/llm-endpoints/{endpointId}/health/check")
    public ResponseEntity<APIResponse<LlmHealthResponse>> triggerHealthCheck(
            @PathVariable UUID endpointId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        LlmHealthResponse response = llmHealthService.checkEndpointHealth(endpointId, user);

        return ResponseEntity.ok(APIResponse.<LlmHealthResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Health check completed")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    /**
     * GET /api/llm-endpoints/{endpointId}/health
     * Get the latest health status without triggering a new check.
     */
    @GetMapping("/api/llm-endpoints/{endpointId}/health")
    public ResponseEntity<APIResponse<LlmHealthResponse>> getLatestHealth(
            @PathVariable UUID endpointId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        LlmHealthResponse response = llmHealthService.getLatestHealth(endpointId, user);

        return ResponseEntity.ok(APIResponse.<LlmHealthResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Latest health status retrieved")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    /**
     * GET /api/llm-endpoints/{endpointId}/health/history
     * Paginated health check history for an endpoint.
     */
    @GetMapping("/api/llm-endpoints/{endpointId}/health/history")
    public ResponseEntity<APIResponse<Page<LlmHealthResponse>>> getHealthHistory(
            @PathVariable UUID endpointId,
            @PageableDefault(size = 50, sort = "checkedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        Page<LlmHealthResponse> response = llmHealthService.getHealthHistory(endpointId, user, pageable);

        return ResponseEntity.ok(APIResponse.<Page<LlmHealthResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Health history retrieved")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    /**
     * GET /api/projects/{projectId}/health/dashboard
     * Dashboard view: all endpoints in a project with their current health.
     */
    @GetMapping("/api/projects/{projectId}/health/dashboard")
    public ResponseEntity<APIResponse<HealthDashboardResponse>> getProjectHealthDashboard(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        HealthDashboardResponse response = llmHealthService.getProjectDashboard(projectId, user);

        return ResponseEntity.ok(APIResponse.<HealthDashboardResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Health dashboard retrieved")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }
}
