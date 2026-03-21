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
import io.github.ashwithpoojary98.llmops_eval.settings.dto.EmailConfigRequest;
import io.github.ashwithpoojary98.llmops_eval.settings.dto.EmailConfigResponse;
import io.github.ashwithpoojary98.llmops_eval.settings.dto.PlatformSettingsRequest;
import io.github.ashwithpoojary98.llmops_eval.settings.dto.PlatformSettingsResponse;
import io.github.ashwithpoojary98.llmops_eval.settings.dto.TestEmailRequest;
import io.github.ashwithpoojary98.llmops_eval.settings.dto.TestEmailResponse;
import io.github.ashwithpoojary98.llmops_eval.settings.service.AdminSettingService;
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
    private final AdminSettingService adminSettingService;

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

    // ──────────────────────────────────────────────────────────────────────────
    // Settings — Email Configuration
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/admin/settings/email
     * Get the current email (SMTP) configuration for the organization.
     */
    @GetMapping("/settings/email")
    public ResponseEntity<APIResponse<EmailConfigResponse>> getEmailConfig(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        UUID orgId = authenticatedUser.getOrganizationIdAsUUID();
        EmailConfigResponse config = adminSettingService.getEmailConfig(orgId);

        return ResponseEntity.ok(APIResponse.<EmailConfigResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Email configuration retrieved")
                .data(config)
                .timestamp(Instant.now())
                .build());
    }

    /**
     * PUT /api/admin/settings/email
     * Save or update the SMTP configuration for the organization.
     */
    @PutMapping("/settings/email")
    public ResponseEntity<APIResponse<EmailConfigResponse>> saveEmailConfig(
            @Valid @RequestBody EmailConfigRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User admin = userService.findById(authenticatedUser.getUserIdAsUUID());
        EmailConfigResponse config = adminSettingService.saveEmailConfig(
                authenticatedUser.getOrganizationIdAsUUID(), request, admin);

        return ResponseEntity.ok(APIResponse.<EmailConfigResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Email configuration saved successfully")
                .data(config)
                .timestamp(Instant.now())
                .build());
    }

    /**
     * PATCH /api/admin/settings/email/toggle
     * Enable or disable email sending without changing SMTP config.
     */
    @PatchMapping("/settings/email/toggle")
    public ResponseEntity<APIResponse<Void>> toggleEmail(
            @RequestParam boolean enabled,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User admin = userService.findById(authenticatedUser.getUserIdAsUUID());
        adminSettingService.toggleEmail(authenticatedUser.getOrganizationIdAsUUID(), enabled, admin);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Email " + (enabled ? "enabled" : "disabled") + " successfully")
                .timestamp(Instant.now())
                .build());
    }

    /**
     * POST /api/admin/settings/email/test
     * Send a test email using the current SMTP configuration.
     */
    @PostMapping("/settings/email/test")
    public ResponseEntity<APIResponse<TestEmailResponse>> testEmailConfig(
            @Valid @RequestBody TestEmailRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        UUID orgId = authenticatedUser.getOrganizationIdAsUUID();
        TestEmailResponse result = adminSettingService.sendTestEmail(orgId, request);

        return ResponseEntity.ok(APIResponse.<TestEmailResponse>builder()
                .success(result.isSuccess())
                .statusCode(HttpStatus.OK.value())
                .message(result.getMessage())
                .data(result)
                .timestamp(Instant.now())
                .build());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Settings — Platform
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/admin/settings/platform
     * Get platform-level settings (base URL, platform name, support email).
     */
    @GetMapping("/settings/platform")
    public ResponseEntity<APIResponse<PlatformSettingsResponse>> getPlatformSettings(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        PlatformSettingsResponse settings = adminSettingService.getPlatformSettings(
                authenticatedUser.getOrganizationIdAsUUID());

        return ResponseEntity.ok(APIResponse.<PlatformSettingsResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Platform settings retrieved")
                .data(settings)
                .timestamp(Instant.now())
                .build());
    }

    /**
     * PUT /api/admin/settings/platform
     * Update platform-level settings.
     */
    @PutMapping("/settings/platform")
    public ResponseEntity<APIResponse<PlatformSettingsResponse>> savePlatformSettings(
            @Valid @RequestBody PlatformSettingsRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User admin = userService.findById(authenticatedUser.getUserIdAsUUID());
        PlatformSettingsResponse settings = adminSettingService.savePlatformSettings(
                authenticatedUser.getOrganizationIdAsUUID(), request, admin);

        return ResponseEntity.ok(APIResponse.<PlatformSettingsResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Platform settings saved successfully")
                .data(settings)
                .timestamp(Instant.now())
                .build());
    }
}
