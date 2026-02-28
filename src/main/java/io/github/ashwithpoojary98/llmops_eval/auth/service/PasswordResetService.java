package io.github.ashwithpoojary98.llmops_eval.auth.service;

import io.github.ashwithpoojary98.llmops_eval.auth.dto.ResetPasswordRequest;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.ResetPasswordTokenValidationResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.PasswordResetToken;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.repository.PasswordResetTokenRepository;
import io.github.ashwithpoojary98.llmops_eval.auth.repository.UserRepository;
import io.github.ashwithpoojary98.llmops_eval.notification.service.EmailService;
import io.github.ashwithpoojary98.llmops_eval.notification.dto.EmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${llmops.base-url:http://localhost:3000}")
    private String baseUrl;

    @Value("${llmops.password-reset.expiration-hours:24}")
    private int expirationHours;

    private static final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void requestPasswordReset(String email) {
        // findActiveByEmail only returns active users
        Optional<User> userOpt = userRepository.findActiveByEmail(email.toLowerCase());

        if (userOpt.isEmpty()) {
            // Don't reveal if email exists or not for security
            log.info("Password reset requested for non-existent or inactive email: {}", email);
            return;
        }

        User user = userOpt.get();

        // Invalidate any existing tokens for this user
        tokenRepository.invalidateAllTokensForUser(user);

        // Generate new token
        String token = generateSecureToken();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(Instant.now().plus(expirationHours, ChronoUnit.HOURS))
                .build();

        tokenRepository.save(resetToken);

        // Send email
        sendPasswordResetEmail(user, token);

        log.info("Password reset token generated for user: {}", email);
    }

    @Transactional(readOnly = true)
    public ResetPasswordTokenValidationResponse validateToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByTokenAndUsedFalse(token);

        if (tokenOpt.isEmpty()) {
            return ResetPasswordTokenValidationResponse.builder()
                    .valid(false)
                    .message("Invalid or expired reset link")
                    .build();
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isExpired()) {
            return ResetPasswordTokenValidationResponse.builder()
                    .valid(false)
                    .message("This reset link has expired")
                    .build();
        }

        // Mask email for privacy
        String email = resetToken.getUser().getEmail();
        String maskedEmail = maskEmail(email);

        return ResetPasswordTokenValidationResponse.builder()
                .valid(true)
                .email(maskedEmail)
                .message("Token is valid")
                .build();
    }

    @Transactional
    public void resetPassword(String token, ResetPasswordRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset link"));

        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("This reset link has expired");
        }

        User user = resetToken.getUser();

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        // Invalidate all other tokens for this user
        tokenRepository.invalidateAllTokensForUser(user);

        log.info("Password successfully reset for user: {}", user.getEmail());

        // Send confirmation email
        sendPasswordChangedEmail(user);
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return email.charAt(0) + "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    private void sendPasswordResetEmail(User user, String token) {
        String resetUrl = baseUrl + "/reset-password/" + token;
        String subject = "Reset Your Password - LLMEvalOps";
        String body = String.format("""
                Hello %s,

                We received a request to reset your password for your LLMEvalOps account.

                Click the link below to reset your password:
                %s

                This link will expire in %d hours.

                If you didn't request a password reset, you can safely ignore this email.
                Your password will remain unchanged.

                Best regards,
                The LLMEvalOps Team
                """,
                user.getFullName(),
                resetUrl,
                expirationHours
        );

        EmailRequest emailRequest = EmailRequest.builder()
                .to(user.getEmail())
                .subject(subject)
                .body(body)
                .isHtml(false)
                .build();

        emailService.send(emailRequest);
    }

    private void sendPasswordChangedEmail(User user) {
        String subject = "Password Changed - LLMEvalOps";
        String body = String.format("""
                Hello %s,

                Your password has been successfully changed.

                If you did not make this change, please contact support immediately.

                Best regards,
                The LLMEvalOps Team
                """,
                user.getFullName()
        );

        EmailRequest emailRequest = EmailRequest.builder()
                .to(user.getEmail())
                .subject(subject)
                .body(body)
                .isHtml(false)
                .build();

        emailService.send(emailRequest);
    }
}
