package io.github.ashwithpoojary98.llmops_eval.llms.controller;

import io.github.ashwithpoojary98.llmops_eval.auth.model.APIResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.security.AuthenticatedUser;
import io.github.ashwithpoojary98.llmops_eval.auth.service.UserService;
import io.github.ashwithpoojary98.llmops_eval.llms.dto.*;
import io.github.ashwithpoojary98.llmops_eval.llms.entity.LlmType;
import io.github.ashwithpoojary98.llmops_eval.llms.service.LlmEndpointService;
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
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LlmEndpointController {

    private final LlmEndpointService llmEndpointService;
    private final UserService userService;

    @PostMapping("/api/projects/{projectId}/llm-endpoints")
    public ResponseEntity<APIResponse<LlmEndpointResponse>> createEndpoint(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateLlmEndpointRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        LlmEndpointResponse response = llmEndpointService.createEndpoint(projectId, request, user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<LlmEndpointResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("LLM endpoint created successfully")
                        .data(response)
                        .timestamp(Instant.now())
                        .build());
    }

    @GetMapping("/api/projects/{projectId}/llm-endpoints")
    public ResponseEntity<APIResponse<Page<LlmEndpointSummaryResponse>>> listEndpoints(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LlmType llmType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        Page<LlmEndpointSummaryResponse> response = llmEndpointService.listEndpoints(
                projectId, includeInactive, search, llmType, user, pageable);

        return ResponseEntity.ok(APIResponse.<Page<LlmEndpointSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("LLM endpoints retrieved successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    /**
     * List all LLM endpoints of a specific type across all accessible projects (for global views).
     */
    @GetMapping("/api/llm-endpoints")
    public ResponseEntity<APIResponse<Page<LlmEndpointSummaryResponse>>> listEndpointsGlobal(
            @RequestParam LlmType type,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        Page<LlmEndpointSummaryResponse> response = llmEndpointService.listEndpointsGlobal(
                type, search, user, pageable);

        return ResponseEntity.ok(APIResponse.<Page<LlmEndpointSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("LLM endpoints retrieved successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    @GetMapping("/api/llm-endpoints/{endpointId}")
    public ResponseEntity<APIResponse<LlmEndpointResponse>> getEndpoint(
            @PathVariable UUID endpointId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        LlmEndpointResponse response = llmEndpointService.getEndpoint(endpointId, user);

        return ResponseEntity.ok(APIResponse.<LlmEndpointResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("LLM endpoint retrieved successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    @PutMapping("/api/llm-endpoints/{endpointId}")
    public ResponseEntity<APIResponse<LlmEndpointResponse>> updateEndpoint(
            @PathVariable UUID endpointId,
            @Valid @RequestBody UpdateLlmEndpointRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        LlmEndpointResponse response = llmEndpointService.updateEndpoint(endpointId, request, user);

        return ResponseEntity.ok(APIResponse.<LlmEndpointResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("LLM endpoint updated successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    @DeleteMapping("/api/llm-endpoints/{endpointId}")
    public ResponseEntity<APIResponse<Void>> deleteEndpoint(
            @PathVariable UUID endpointId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        llmEndpointService.deleteEndpoint(endpointId, user);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("LLM endpoint deleted successfully")
                .timestamp(Instant.now())
                .build());
    }
}
