package io.github.ashwithpoojary98.llmops_eval.evaluation.controller;

import io.github.ashwithpoojary98.llmops_eval.auth.model.APIResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.security.AuthenticatedUser;
import io.github.ashwithpoojary98.llmops_eval.auth.service.UserService;
import io.github.ashwithpoojary98.llmops_eval.evaluation.dto.*;
import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.EvaluationStatus;
import io.github.ashwithpoojary98.llmops_eval.evaluation.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@RequiredArgsConstructor
public class EvaluationController {

    @Value("${evaluation.engine.callback-secret:}")
    private String callbackSecret;

    private final EvaluationService evaluationService;
    private final UserService userService;

    /**
     * Start a new evaluation run.
     */
    @PostMapping("/api/projects/{projectId}/evaluations")
    public ResponseEntity<APIResponse<EvaluationRunResponse>> startEvaluation(
            @PathVariable UUID projectId,
            @Valid @RequestBody StartEvaluationRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        EvaluationRunResponse response = evaluationService.startEvaluation(projectId, request, user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<EvaluationRunResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Evaluation started successfully")
                        .data(response)
                        .timestamp(Instant.now())
                        .build());
    }

    /**
     * List evaluation runs for a project.
     */
    @GetMapping("/api/projects/{projectId}/evaluations")
    public ResponseEntity<APIResponse<Page<EvaluationRunSummaryResponse>>> listEvaluations(
            @PathVariable UUID projectId,
            @RequestParam(required = false) EvaluationStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        Page<EvaluationRunSummaryResponse> response = evaluationService.listEvaluationRuns(
                projectId, status, search, user, pageable);

        return ResponseEntity.ok(APIResponse.<Page<EvaluationRunSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Evaluations retrieved successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    /**
     * Get evaluation run details.
     */
    @GetMapping("/api/evaluations/{evaluationId}")
    public ResponseEntity<APIResponse<EvaluationRunResponse>> getEvaluation(
            @PathVariable UUID evaluationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        EvaluationRunResponse response = evaluationService.getEvaluationRun(evaluationId, user);

        return ResponseEntity.ok(APIResponse.<EvaluationRunResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Evaluation retrieved successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    /**
     * Get evaluation results (per test case).
     */
    @GetMapping("/api/evaluations/{evaluationId}/results")
    public ResponseEntity<APIResponse<Page<EvaluationResultResponse>>> getEvaluationResults(
            @PathVariable UUID evaluationId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        Page<EvaluationResultResponse> response = evaluationService.getEvaluationResults(
                evaluationId, user, pageable);

        return ResponseEntity.ok(APIResponse.<Page<EvaluationResultResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Evaluation results retrieved successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    /**
     * Cancel a running evaluation.
     */
    @PostMapping("/api/evaluations/{evaluationId}/cancel")
    public ResponseEntity<APIResponse<Void>> cancelEvaluation(
            @PathVariable UUID evaluationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        evaluationService.cancelEvaluation(evaluationId, user);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Evaluation cancelled successfully")
                .timestamp(Instant.now())
                .build());
    }

    /**
     * Delete an evaluation run permanently.
     */
    @DeleteMapping("/api/evaluations/{evaluationId}")
    public ResponseEntity<APIResponse<Void>> deleteEvaluation(
            @PathVariable UUID evaluationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        evaluationService.deleteEvaluation(evaluationId, user);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Evaluation deleted successfully")
                .timestamp(Instant.now())
                .build());
    }

    /**
     * Retrigger an evaluation with the same configuration.
     */
    @PostMapping("/api/evaluations/{evaluationId}/retrigger")
    public ResponseEntity<APIResponse<EvaluationRunResponse>> retriggerEvaluation(
            @PathVariable UUID evaluationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        EvaluationRunResponse response = evaluationService.retriggerEvaluation(evaluationId, user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<EvaluationRunResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Evaluation retriggered successfully")
                        .data(response)
                        .timestamp(Instant.now())
                        .build());
    }

    /**
     * Get available metrics for a project.
     */
    @GetMapping("/api/projects/{projectId}/metrics")
    public ResponseEntity<APIResponse<List<EvaluationMetricResponse>>> getAvailableMetrics(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        List<EvaluationMetricResponse> response = evaluationService.getAvailableMetrics(projectId, user);

        return ResponseEntity.ok(APIResponse.<List<EvaluationMetricResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Metrics retrieved successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    /**
     * Internal callback endpoint called by the evaluation engine on completion.
     * Protected by a shared secret sent in X-Callback-Secret header.
     */
    @PostMapping("/api/evaluations/callback/{evaluationId}")
    public ResponseEntity<Void> evaluationCallback(
            @PathVariable String evaluationId,
            @RequestHeader(value = "X-Callback-Secret", required = false) String providedSecret,
            @RequestBody @Valid EvaluationCallbackPayload payload
    ) {
        if (callbackSecret != null && !callbackSecret.isBlank()) {
            if (providedSecret == null || !callbackSecret.equals(providedSecret)) {
                log.warn("Rejected callback for run {} — invalid or missing X-Callback-Secret", evaluationId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }
        log.info("Received evaluation callback for run: {}", evaluationId);
        evaluationService.handleEvaluationCallback(evaluationId, payload);
        return ResponseEntity.ok().build();
    }
}
