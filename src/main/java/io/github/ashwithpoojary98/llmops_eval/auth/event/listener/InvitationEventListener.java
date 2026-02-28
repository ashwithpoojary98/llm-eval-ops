package io.github.ashwithpoojary98.llmops_eval.auth.event.listener;

import io.github.ashwithpoojary98.llmops_eval.auth.event.InvitationAcceptedEvent;
import io.github.ashwithpoojary98.llmops_eval.auth.event.UserDeactivatedEvent;
import io.github.ashwithpoojary98.llmops_eval.auth.event.UserInvitedEvent;
import io.github.ashwithpoojary98.llmops_eval.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvitationEventListener {

    private final EmailService emailService;

    /**
     * Handle user invited event - sends invitation email
     * Executes AFTER transaction commits to ensure data is persisted
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("emailTaskExecutor")
    public void handleUserInvitedEvent(UserInvitedEvent event) {
        log.info("Processing UserInvitedEvent for email: {}", event.getEmail());

        try {
            emailService.sendInvitationEmail(
                    event.getEmail(),
                    event.getInvitedByName(),
                    event.getOrganizationName(),
                    event.getInvitationToken(),
                    event.getRole(),
                    event.getExpiresAt()
            );
            log.info("Invitation email sent successfully to: {}", event.getEmail());

        } catch (Exception e) {
            log.error("Failed to send invitation email to: {}", event.getEmail(), e);
            // Don't rethrow - email failure shouldn't affect the main transaction
        }
    }

    /**
     * Handle invitation accepted event - sends welcome email
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("emailTaskExecutor")
    public void handleInvitationAcceptedEvent(InvitationAcceptedEvent event) {
        log.info("Processing InvitationAcceptedEvent for user: {}", event.getEmail());

        try {
            emailService.sendWelcomeEmail(
                    event.getEmail(),
                    event.getFullName(),
                    event.getOrganizationName(),
                    event.getRole()
            );
            log.info("Welcome email sent successfully to: {}", event.getEmail());

        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", event.getEmail(), e);
        }
    }

    /**
     * Handle user deactivated event - sends notification email
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("emailTaskExecutor")
    public void handleUserDeactivatedEvent(UserDeactivatedEvent event) {
        log.info("Processing UserDeactivatedEvent for user: {}", event.getEmail());

        try {
            emailService.sendAccountDeactivatedEmail(
                    event.getEmail(),
                    event.getFullName(),
                    event.getOrganizationName()
            );
            log.info("Deactivation email sent successfully to: {}", event.getEmail());

        } catch (Exception e) {
            log.error("Failed to send deactivation email to: {}", event.getEmail(), e);
        }
    }
}
