package io.github.ashwithpoojary98.llmops_eval.dataset.controller;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.model.APIResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.security.AuthenticatedUser;
import io.github.ashwithpoojary98.llmops_eval.auth.service.UserService;
import io.github.ashwithpoojary98.llmops_eval.dataset.dto.AddTestCasesRequest;
import io.github.ashwithpoojary98.llmops_eval.dataset.dto.CreateDatasetRequest;
import io.github.ashwithpoojary98.llmops_eval.dataset.dto.DatasetResponse;
import io.github.ashwithpoojary98.llmops_eval.dataset.dto.DatasetSummaryResponse;
import io.github.ashwithpoojary98.llmops_eval.dataset.dto.RemoveTestCasesRequest;
import io.github.ashwithpoojary98.llmops_eval.dataset.dto.UpdateDatasetRequest;
import io.github.ashwithpoojary98.llmops_eval.dataset.service.DatasetService;
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
public class DatasetController {

    private final DatasetService datasetService;
    private final UserService userService;

    @PostMapping("/api/projects/{projectId}/datasets")
    public ResponseEntity<APIResponse<DatasetResponse>> createDataset(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateDatasetRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        DatasetResponse response = datasetService.createDataset(projectId, request, user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<DatasetResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Dataset created successfully")
                        .data(response)
                        .timestamp(Instant.now())
                        .build());
    }

    @GetMapping("/api/projects/{projectId}/datasets")
    public ResponseEntity<APIResponse<Page<DatasetSummaryResponse>>> listDatasets(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        Page<DatasetSummaryResponse> datasets = datasetService.listDatasets(
                projectId, includeInactive, search, user, pageable
        );

        return ResponseEntity.ok(APIResponse.<Page<DatasetSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .data(datasets)
                .timestamp(Instant.now())
                .build());
    }

    @GetMapping("/api/datasets")
    public ResponseEntity<APIResponse<Page<DatasetSummaryResponse>>> listAllDatasets(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        Page<DatasetSummaryResponse> datasets = datasetService.listAllDatasets(
                includeInactive, search, user, pageable
        );

        return ResponseEntity.ok(APIResponse.<Page<DatasetSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .data(datasets)
                .timestamp(Instant.now())
                .build());
    }

    @GetMapping("/api/datasets/{id}")
    public ResponseEntity<APIResponse<DatasetResponse>> getDataset(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "true") boolean includeTestCases
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        DatasetResponse response = datasetService.getDataset(id, user, includeTestCases);

        return ResponseEntity.ok(APIResponse.<DatasetResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    @PutMapping("/api/datasets/{id}")
    public ResponseEntity<APIResponse<DatasetResponse>> updateDataset(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDatasetRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        DatasetResponse response = datasetService.updateDataset(id, request, user);

        return ResponseEntity.ok(APIResponse.<DatasetResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Dataset updated successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    @DeleteMapping("/api/datasets/{id}")
    public ResponseEntity<APIResponse<Void>> deleteDataset(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        datasetService.deleteDataset(id, user);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Dataset deleted successfully")
                .timestamp(Instant.now())
                .build());
    }

    @PostMapping("/api/datasets/{id}/reactivate")
    public ResponseEntity<APIResponse<DatasetResponse>> reactivateDataset(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        DatasetResponse response = datasetService.reactivateDataset(id, user);

        return ResponseEntity.ok(APIResponse.<DatasetResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Dataset reactivated successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    @PostMapping("/api/datasets/{id}/testcases")
    public ResponseEntity<APIResponse<DatasetResponse>> addTestCases(
            @PathVariable UUID id,
            @Valid @RequestBody AddTestCasesRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        DatasetResponse response = datasetService.addTestCases(id, request, user);

        return ResponseEntity.ok(APIResponse.<DatasetResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Test cases added successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    @DeleteMapping("/api/datasets/{id}/testcases")
    public ResponseEntity<APIResponse<DatasetResponse>> removeTestCases(
            @PathVariable UUID id,
            @Valid @RequestBody RemoveTestCasesRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        DatasetResponse response = datasetService.removeTestCases(id, request, user);

        return ResponseEntity.ok(APIResponse.<DatasetResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Test cases removed successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }
}
