package io.github.ashwithpoojary98.llmops_eval.testcases.controller;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.model.APIResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.security.AuthenticatedUser;
import io.github.ashwithpoojary98.llmops_eval.auth.service.UserService;
import io.github.ashwithpoojary98.llmops_eval.testcases.dto.CreateTestCaseRequest;
import io.github.ashwithpoojary98.llmops_eval.testcases.dto.TestCaseResponse;
import io.github.ashwithpoojary98.llmops_eval.testcases.dto.TestCaseSummaryResponse;
import io.github.ashwithpoojary98.llmops_eval.testcases.dto.UpdateTestCaseRequest;
import io.github.ashwithpoojary98.llmops_eval.testcases.services.TestCaseService;
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
public class TestCaseController {

    private final TestCaseService testCaseService;
    private final UserService userService;

    @PostMapping("/api/projects/{projectId}/testcases")
    public ResponseEntity<APIResponse<TestCaseResponse>> createTestCase(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTestCaseRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        TestCaseResponse response = testCaseService.createTestCase(projectId, request, user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<TestCaseResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Test case created successfully")
                        .data(response)
                        .timestamp(Instant.now())
                        .build());
    }

    @GetMapping("/api/projects/{projectId}/testcases")
    public ResponseEntity<APIResponse<Page<TestCaseSummaryResponse>>> listTestCases(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        Page<TestCaseSummaryResponse> testCases = testCaseService.listTestCases(
                projectId, includeInactive, search, user, pageable
        );

        return ResponseEntity.ok(APIResponse.<Page<TestCaseSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .data(testCases)
                .timestamp(Instant.now())
                .build());
    }

    @GetMapping("/api/testcases")
    public ResponseEntity<APIResponse<Page<TestCaseResponse>>> listAllTestCases(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        Page<TestCaseResponse> testCases = testCaseService.listAllTestCases(
                includeInactive, search, user, pageable
        );

        return ResponseEntity.ok(APIResponse.<Page<TestCaseResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .data(testCases)
                .timestamp(Instant.now())
                .build());
    }

    @GetMapping("/api/testcases/{id}")
    public ResponseEntity<APIResponse<TestCaseResponse>> getTestCase(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        TestCaseResponse response = testCaseService.getTestCase(id, user);

        return ResponseEntity.ok(APIResponse.<TestCaseResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    @PutMapping("/api/testcases/{id}")
    public ResponseEntity<APIResponse<TestCaseResponse>> updateTestCase(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTestCaseRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        TestCaseResponse response = testCaseService.updateTestCase(id, request, user);

        return ResponseEntity.ok(APIResponse.<TestCaseResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Test case updated successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    @DeleteMapping("/api/testcases/{id}")
    public ResponseEntity<APIResponse<Void>> deleteTestCase(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        testCaseService.deleteTestCase(id, user);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Test case deleted successfully")
                .timestamp(Instant.now())
                .build());
    }

    @PostMapping("/api/testcases/{id}/reactivate")
    public ResponseEntity<APIResponse<TestCaseResponse>> reactivateTestCase(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        TestCaseResponse response = testCaseService.reactivateTestCase(id, user);

        return ResponseEntity.ok(APIResponse.<TestCaseResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Test case reactivated successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }
}
