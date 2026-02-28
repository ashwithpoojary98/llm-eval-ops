package io.github.ashwithpoojary98.llmops_eval.auth.controller;

import io.github.ashwithpoojary98.llmops_eval.auth.dto.LoginRequest;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.LoginResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.LogoutRequest;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.RefreshTokenRequest;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.UpdateUserPasswordRequest;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.ForgotPasswordRequest;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.ResetPasswordRequest;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.ResetPasswordTokenValidationResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.model.APIResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.security.AuthenticatedUser;
import io.github.ashwithpoojary98.llmops_eval.auth.service.AuthService;
import io.github.ashwithpoojary98.llmops_eval.auth.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @GetMapping("/health")
    public ResponseEntity<APIResponse<Void>> healthCheck() {
        return ResponseEntity.ok(
                APIResponse.<Void>builder()
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Auth service is healthy")
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request
    ) {
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        LoginResponse response = authService.login(loginRequest, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest
    ) {
        LoginResponse response = authService.refreshAccessToken(refreshTokenRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody(required = false) LogoutRequest logoutRequest,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        String refreshToken = logoutRequest != null ? logoutRequest.getRefreshToken() : null;
        UUID accessUserId = authenticatedUser.getUserIdAsUUID();
        authService.logout(accessUserId, refreshToken);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }


    @PatchMapping("/update-password")
    public ResponseEntity<APIResponse<Void>> updatePassword(
            @Valid @RequestBody UpdateUserPasswordRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        authService.updatePassword(user.getEmail(), request);
        return ResponseEntity.ok(
                APIResponse.<Void>builder()
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Password updated successfully")
                        .build()
        );
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<APIResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        passwordResetService.requestPasswordReset(request.getEmail());
        // Always return success to prevent email enumeration
        return ResponseEntity.ok(
                APIResponse.<Void>builder()
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("If an account with that email exists, a password reset link has been sent")
                        .build()
        );
    }

    @GetMapping("/reset-password/{token}")
    public ResponseEntity<APIResponse<ResetPasswordTokenValidationResponse>> validateResetToken(
            @PathVariable String token
    ) {
        ResetPasswordTokenValidationResponse response = passwordResetService.validateToken(token);
        return ResponseEntity.ok(
                APIResponse.<ResetPasswordTokenValidationResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/reset-password/{token}")
    public ResponseEntity<APIResponse<Void>> resetPassword(
            @PathVariable String token,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        passwordResetService.resetPassword(token, request);
        return ResponseEntity.ok(
                APIResponse.<Void>builder()
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Password has been reset successfully")
                        .build()
        );
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

}
