package io.github.ashwithpoojary98.llmops_eval.auth.controller;

import io.github.ashwithpoojary98.llmops_eval.auth.dto.UserSummaryResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.model.APIResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.security.AuthenticatedUser;
import io.github.ashwithpoojary98.llmops_eval.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<APIResponse<Page<UserSummaryResponse>>> listOrganizationUsers(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PageableDefault(size = 100, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        UUID orgId = authenticatedUser.getOrganizationIdAsUUID();
        Page<UserSummaryResponse> users = userService.listUsersByOrganization(orgId, pageable);

        return ResponseEntity.ok(APIResponse.<Page<UserSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .data(users)
                .timestamp(Instant.now())
                .build());
    }
}
