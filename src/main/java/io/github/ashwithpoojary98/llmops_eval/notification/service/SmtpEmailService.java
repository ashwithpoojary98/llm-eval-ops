package io.github.ashwithpoojary98.llmops_eval.notification.service;

import io.github.ashwithpoojary98.llmops_eval.notification.dto.EmailRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@llmeval.io}")
    private String fromEmail;

    @Value("${spring.mail.from-name:LLMEvalDevOps}")
    private String fromName;

    @Value("${llmops.app.base-url:http://localhost:3000}")
    private String appBaseUrl;

    @Override
    public void send(EmailRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setText(request.getBody(), request.isHtml());

            if (request.getCc() != null && !request.getCc().isEmpty()) {
                helper.setCc(request.getCc().toArray(new String[0]));
            }

            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                helper.setBcc(request.getBcc().toArray(new String[0]));
            }

            mailSender.send(message);
            log.info("Email sent successfully to: {}", request.getTo());

        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", request.getTo(), e);
            throw new RuntimeException("Failed to send email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to: {}", request.getTo(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendAsync(EmailRequest request) {
        try {
            send(request);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public void sendInvitationEmail(
            String toEmail,
            String inviterName,
            String organizationName,
            String invitationToken,
            String role,
            Instant expiresAt
    ) {
        String inviteUrl = appBaseUrl + "/invite/" + invitationToken;
        String expiryDate = DateTimeFormatter
                .ofPattern("MMMM dd, yyyy 'at' HH:mm z")
                .withZone(ZoneId.systemDefault())
                .format(expiresAt);

        String subject = "You're invited to join " + organizationName + " on LLMEvalDevOps";

        String body = buildInvitationEmailHtml(
                inviterName,
                organizationName,
                role,
                inviteUrl,
                expiryDate
        );

        EmailRequest request = EmailRequest.builder()
                .to(toEmail)
                .subject(subject)
                .body(body)
                .isHtml(true)
                .build();

        sendAsync(request);
    }

    @Override
    public void sendWelcomeEmail(
            String toEmail,
            String fullName,
            String organizationName,
            String role
    ) {
        String subject = "Welcome to " + organizationName + " on LLMEvalDevOps!";

        String body = buildWelcomeEmailHtml(fullName, organizationName, role);

        EmailRequest request = EmailRequest.builder()
                .to(toEmail)
                .subject(subject)
                .body(body)
                .isHtml(true)
                .build();

        sendAsync(request);
    }

    @Override
    public void sendAccountDeactivatedEmail(
            String toEmail,
            String fullName,
            String organizationName
    ) {
        String subject = "Your " + organizationName + " account has been deactivated";

        String body = buildDeactivatedEmailHtml(fullName, organizationName);

        EmailRequest request = EmailRequest.builder()
                .to(toEmail)
                .subject(subject)
                .body(body)
                .isHtml(true)
                .build();

        sendAsync(request);
    }

    private String buildInvitationEmailHtml(
            String inviterName,
            String organizationName,
            String role,
            String inviteUrl,
            String expiryDate
    ) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 24px;">LLMEvalDevOps</h1>
                    </div>
                    <div style="background: #ffffff; padding: 30px; border: 1px solid #e0e0e0; border-top: none; border-radius: 0 0 10px 10px;">
                        <h2 style="color: #333; margin-top: 0;">You're Invited!</h2>
                        <p><strong>%s</strong> has invited you to join <strong>%s</strong> on LLMEvalDevOps as a <strong>%s</strong>.</p>

                        <p>LLMEvalDevOps is a platform for evaluating LLM and RAG applications with ease.</p>

                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 14px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;">Accept Invitation</a>
                        </div>

                        <p style="color: #666; font-size: 14px;">This invitation expires on <strong>%s</strong>.</p>

                        <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;">

                        <p style="color: #999; font-size: 12px;">If you didn't expect this invitation, you can safely ignore this email.</p>

                        <p style="color: #999; font-size: 12px;">Button not working? Copy and paste this link into your browser:<br>
                        <a href="%s" style="color: #667eea; word-break: break-all;">%s</a></p>
                    </div>
                    <div style="text-align: center; padding: 20px; color: #999; font-size: 12px;">
                        <p>&copy; 2025 LLMEvalDevOps. All rights reserved.</p>
                    </div>
                </body>
                </html>
                """.formatted(inviterName, organizationName, role, inviteUrl, expiryDate, inviteUrl, inviteUrl);
    }

    private String buildWelcomeEmailHtml(String fullName, String organizationName, String role) {
        String loginUrl = appBaseUrl + "/login";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 24px;">Welcome to LLMEvalDevOps!</h1>
                    </div>
                    <div style="background: #ffffff; padding: 30px; border: 1px solid #e0e0e0; border-top: none; border-radius: 0 0 10px 10px;">
                        <h2 style="color: #333; margin-top: 0;">Hello, %s! 👋</h2>

                        <p>Your account has been successfully created. You're now a <strong>%s</strong> at <strong>%s</strong>.</p>

                        <h3 style="color: #667eea;">What's Next?</h3>
                        <ul style="color: #666;">
                            <li>Log in to your account</li>
                            <li>Explore the dashboard</li>
                            <li>Create your first evaluation project</li>
                            <li>Start evaluating your LLM applications</li>
                        </ul>

                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 14px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;">Go to Dashboard</a>
                        </div>

                        <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;">

                        <p style="color: #999; font-size: 12px;">Need help? Contact your organization administrator.</p>
                    </div>
                    <div style="text-align: center; padding: 20px; color: #999; font-size: 12px;">
                        <p>&copy; 2025 LLMEvalDevOps. All rights reserved.</p>
                    </div>
                </body>
                </html>
                """.formatted(fullName, role, organizationName, loginUrl);
    }

    private String buildDeactivatedEmailHtml(String fullName, String organizationName) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: #f44336; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 24px;">Account Deactivated</h1>
                    </div>
                    <div style="background: #ffffff; padding: 30px; border: 1px solid #e0e0e0; border-top: none; border-radius: 0 0 10px 10px;">
                        <h2 style="color: #333; margin-top: 0;">Hello, %s</h2>

                        <p>Your account at <strong>%s</strong> on LLMEvalDevOps has been deactivated by an administrator.</p>

                        <p>You will no longer be able to log in or access the platform.</p>

                        <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;">

                        <p style="color: #999; font-size: 12px;">If you believe this was done in error, please contact your organization administrator.</p>
                    </div>
                    <div style="text-align: center; padding: 20px; color: #999; font-size: 12px;">
                        <p>&copy; 2025 LLMEvalDevOps. All rights reserved.</p>
                    </div>
                </body>
                </html>
                """.formatted(fullName, organizationName);
    }
}
