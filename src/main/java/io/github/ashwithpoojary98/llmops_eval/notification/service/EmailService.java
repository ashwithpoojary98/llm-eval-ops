package io.github.ashwithpoojary98.llmops_eval.notification.service;

import io.github.ashwithpoojary98.llmops_eval.notification.dto.EmailRequest;

import java.util.concurrent.CompletableFuture;

public interface EmailService {

    /**
     * Send email synchronously
     */
    void send(EmailRequest request);

    /**
     * Send email asynchronously
     */
    CompletableFuture<Void> sendAsync(EmailRequest request);

    /**
     * Send invitation email
     */
    void sendInvitationEmail(
            String toEmail,
            String inviterName,
            String organizationName,
            String invitationToken,
            String role,
            java.time.Instant expiresAt
    );

    /**
     * Send welcome email after invitation accepted
     */
    void sendWelcomeEmail(
            String toEmail,
            String fullName,
            String organizationName,
            String role
    );

    /**
     * Send account deactivated notification
     */
    void sendAccountDeactivatedEmail(
            String toEmail,
            String fullName,
            String organizationName
    );
}
