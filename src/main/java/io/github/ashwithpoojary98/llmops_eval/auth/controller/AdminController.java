package io.github.ashwithpoojary98.llmops_eval.auth.controller;

import io.github.ashwithpoojary98.llmops_eval.auth.dto.InvitationSummaryResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.InviteUserRequest;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.InviteUserResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.UpdateUserRoleRequest;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.UserSummaryResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.model.APIResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.security.AuthenticatedUser;
import io.github.ashwithpoojary98.llmops_eval.auth.service.InvitationService;
import io.github.ashwithpoojary98.llmops_eval.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final InvitationService invitationService;
    private final UserService userService;

    @PostMapping("/invite")
    public ResponseEntity<APIResponse<InviteUserResponse>> inviteUser(
            @Valid @RequestBody InviteUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User admin = userService.findById(authenticatedUser.getUserIdAsUUID());
        InviteUserResponse response = invitationService.inviteUser(request, admin);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<InviteUserResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Invitation sent successfully")
                        .data(response)
                        .timestamp(Instant.now())
                        .build());
    }

    @GetMapping("/invitations")
    public ResponseEntity<APIResponse<Page<InvitationSummaryResponse>>> listInvitations(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "all") String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID orgId = authenticatedUser.getOrganizationIdAsUUID();

        Page<InvitationSummaryResponse> invitations = invitationService.listInvitationsByStatus(orgId, status, pageable);

        return ResponseEntity.ok(APIResponse.<Page<InvitationSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .data(invitations)
                .timestamp(Instant.now())
                .build());
    }

    @DeleteMapping("/invitations/{id}")
    public ResponseEntity<APIResponse<Void>> revokeInvitation(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        invitationService.revokeInvitation(id, authenticatedUser.getUserIdAsUUID());

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Invitation revoked successfully")
                .timestamp(Instant.now())
                .build());
    }


    @GetMapping("/users")
    public ResponseEntity<APIResponse<Page<UserSummaryResponse>>> listUsers(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
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

    @GetMapping("/users/{id}")
    public ResponseEntity<APIResponse<UserSummaryResponse>> getUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        UserSummaryResponse user = userService.getUserDetails(id, authenticatedUser.getUserIdAsUUID());

        return ResponseEntity.ok(APIResponse.<UserSummaryResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .data(user)
                .timestamp(Instant.now())
                .build());
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<APIResponse<UserSummaryResponse>> updateUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User admin = userService.findById(authenticatedUser.getUserIdAsUUID());
        UserSummaryResponse user = userService.updateUserRole(id, request, admin);

        return ResponseEntity.ok(APIResponse.<UserSummaryResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("User role updated successfully")
                .data(user)
                .timestamp(Instant.now())
                .build());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<APIResponse<Void>> deactivateUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User admin = userService.findById(authenticatedUser.getUserIdAsUUID());
        userService.deactivateUser(id, admin);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("User deactivated successfully")
                .timestamp(Instant.now())
                .build());
    }

    @PostMapping("/users/{id}/reactivate")
    public ResponseEntity<APIResponse<UserSummaryResponse>> reactivateUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User admin = userService.findById(authenticatedUser.getUserIdAsUUID());
        UserSummaryResponse user = userService.reactivateUser(id, admin);

        return ResponseEntity.ok(APIResponse.<UserSummaryResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("User reactivated successfully")
                .data(user)
                .timestamp(Instant.now())
                .build());
    }
}
