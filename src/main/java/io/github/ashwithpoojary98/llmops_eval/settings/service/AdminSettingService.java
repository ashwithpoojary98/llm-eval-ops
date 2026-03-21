package io.github.ashwithpoojary98.llmops_eval.settings.service;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.Organization;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.llms.service.EncryptionService;
import io.github.ashwithpoojary98.llmops_eval.settings.dto.EmailConfigRequest;
import io.github.ashwithpoojary98.llmops_eval.settings.dto.EmailConfigResponse;
import io.github.ashwithpoojary98.llmops_eval.settings.dto.PlatformSettingsRequest;
import io.github.ashwithpoojary98.llmops_eval.settings.dto.PlatformSettingsResponse;
import io.github.ashwithpoojary98.llmops_eval.settings.dto.TestEmailRequest;
import io.github.ashwithpoojary98.llmops_eval.settings.dto.TestEmailResponse;
import io.github.ashwithpoojary98.llmops_eval.settings.entity.AdminSetting;
import io.github.ashwithpoojary98.llmops_eval.settings.entity.SettingKey;
import io.github.ashwithpoojary98.llmops_eval.settings.repository.AdminSettingRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminSettingService {

    private final AdminSettingRepository settingRepository;
    private final EncryptionService encryptionService;

    // Fallback values from application.yaml (used when DB has no config yet)
    @Value("${spring.mail.host:smtp.gmail.com}")
    private String defaultSmtpHost;

    @Value("${spring.mail.port:587}")
    private int defaultSmtpPort;

    @Value("${spring.mail.username:}")
    private String defaultSmtpUsername;

    @Value("${spring.mail.password:}")
    private String defaultSmtpPassword;

    @Value("${spring.mail.from:noreply@llmops.io}")
    private String defaultFromAddress;

    @Value("${spring.mail.from-name:LLMOps Platform}")
    private String defaultFromName;

    @Value("${llmops.app.base-url:http://localhost:3000}")
    private String defaultBaseUrl;

    // ──────────────────────────────────────────────────────────────────────────
    // Email Configuration
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional
    public EmailConfigResponse saveEmailConfig(UUID orgId, EmailConfigRequest request, User updatedBy) {
        upsert(orgId, SettingKey.EMAIL_SMTP_HOST, request.getSmtpHost(), false, updatedBy);
        upsert(orgId, SettingKey.EMAIL_SMTP_PORT, String.valueOf(request.getSmtpPort()), false, updatedBy);
        upsert(orgId, SettingKey.EMAIL_FROM_ADDRESS, request.getFromAddress(), false, updatedBy);
        upsert(orgId, SettingKey.EMAIL_FROM_NAME, request.getFromName(), false, updatedBy);
        upsert(orgId, SettingKey.EMAIL_ENABLE_TLS, String.valueOf(request.isEnableTls()), false, updatedBy);
        upsert(orgId, SettingKey.EMAIL_ENABLE_SSL, String.valueOf(request.isEnableSsl()), false, updatedBy);
        upsert(orgId, SettingKey.EMAIL_CONNECTION_TIMEOUT_MS, String.valueOf(request.getConnectionTimeoutMs()), false, updatedBy);

        if (request.getSmtpUsername() != null && !request.getSmtpUsername().isBlank()) {
            upsert(orgId, SettingKey.EMAIL_SMTP_USERNAME, request.getSmtpUsername(), false, updatedBy);
        }

        // Only update password if a new one is provided
        if (request.getSmtpPassword() != null && !request.getSmtpPassword().isBlank()) {
            upsertEncrypted(orgId, SettingKey.EMAIL_SMTP_PASSWORD, request.getSmtpPassword(), updatedBy);
        }

        // Auto-enable email when config is saved
        upsert(orgId, SettingKey.EMAIL_ENABLED, "true", false, updatedBy);

        log.info("Email configuration saved for org {}", orgId);
        return getEmailConfig(orgId);
    }

    @Transactional(readOnly = true)
    public EmailConfigResponse getEmailConfig(UUID orgId) {
        Map<String, AdminSetting> settings = loadSettingMap(orgId, "email.");

        String host = getValue(settings, SettingKey.EMAIL_SMTP_HOST, defaultSmtpHost);
        boolean configured = host != null && !host.isBlank() && !host.equals(defaultSmtpHost);

        Instant lastUpdated = settings.values().stream()
                .map(AdminSetting::getUpdatedAt)
                .filter(t -> t != null)
                .max(Instant::compareTo)
                .orElse(null);

        AdminSetting passwordSetting = settings.get(SettingKey.EMAIL_SMTP_PASSWORD.getKey());

        return EmailConfigResponse.builder()
                .smtpHost(host)
                .smtpPort(getIntValue(settings, SettingKey.EMAIL_SMTP_PORT, defaultSmtpPort))
                .smtpUsername(getValue(settings, SettingKey.EMAIL_SMTP_USERNAME, defaultSmtpUsername))
                .hasPassword(passwordSetting != null && passwordSetting.getEncryptedValue() != null)
                .fromAddress(getValue(settings, SettingKey.EMAIL_FROM_ADDRESS, defaultFromAddress))
                .fromName(getValue(settings, SettingKey.EMAIL_FROM_NAME, defaultFromName))
                .enableTls(getBoolValue(settings, SettingKey.EMAIL_ENABLE_TLS, true))
                .enableSsl(getBoolValue(settings, SettingKey.EMAIL_ENABLE_SSL, false))
                .connectionTimeoutMs(getIntValue(settings, SettingKey.EMAIL_CONNECTION_TIMEOUT_MS, 5000))
                .emailEnabled(getBoolValue(settings, SettingKey.EMAIL_ENABLED, false))
                .configured(configured)
                .lastUpdatedAt(lastUpdated)
                .build();
    }

    @Transactional
    public void toggleEmail(UUID orgId, boolean enabled, User updatedBy) {
        upsert(orgId, SettingKey.EMAIL_ENABLED, String.valueOf(enabled), false, updatedBy);
        log.info("Email {} for org {}", enabled ? "enabled" : "disabled", orgId);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test Email
    // ──────────────────────────────────────────────────────────────────────────

    public TestEmailResponse sendTestEmail(UUID orgId, TestEmailRequest request) {
        long start = System.currentTimeMillis();
        try {
            JavaMailSenderImpl mailSender = buildMailSender(orgId);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            Map<String, AdminSetting> settings = loadSettingMap(orgId, "email.");
            String fromAddress = getValue(settings, SettingKey.EMAIL_FROM_ADDRESS, defaultFromAddress);
            String fromName = getValue(settings, SettingKey.EMAIL_FROM_NAME, defaultFromName);

            helper.setFrom(fromAddress, fromName);
            helper.setTo(request.getRecipientEmail());
            helper.setSubject("LLMOps Platform — Test Email");
            helper.setText(buildTestEmailBody(fromName), true);

            mailSender.send(message);

            long latency = System.currentTimeMillis() - start;
            log.info("Test email sent to {} for org {} in {}ms", request.getRecipientEmail(), orgId, latency);

            return TestEmailResponse.builder()
                    .success(true)
                    .message("Test email delivered successfully to " + request.getRecipientEmail())
                    .latencyMs(latency)
                    .build();

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("Test email failed for org {}: {}", orgId, e.getMessage());

            return TestEmailResponse.builder()
                    .success(false)
                    .message("Failed to send test email")
                    .errorDetail(e.getMessage())
                    .latencyMs(latency)
                    .build();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Platform Settings
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional
    public PlatformSettingsResponse savePlatformSettings(UUID orgId, PlatformSettingsRequest request, User updatedBy) {
        if (request.getBaseUrl() != null) {
            upsert(orgId, SettingKey.PLATFORM_BASE_URL, request.getBaseUrl(), false, updatedBy);
        }
        if (request.getPlatformName() != null) {
            upsert(orgId, SettingKey.PLATFORM_NAME, request.getPlatformName(), false, updatedBy);
        }
        if (request.getSupportEmail() != null) {
            upsert(orgId, SettingKey.PLATFORM_SUPPORT_EMAIL, request.getSupportEmail(), false, updatedBy);
        }
        return getPlatformSettings(orgId);
    }

    @Transactional(readOnly = true)
    public PlatformSettingsResponse getPlatformSettings(UUID orgId) {
        Map<String, AdminSetting> settings = loadSettingMap(orgId, "platform.");
        return PlatformSettingsResponse.builder()
                .baseUrl(getValue(settings, SettingKey.PLATFORM_BASE_URL, defaultBaseUrl))
                .platformName(getValue(settings, SettingKey.PLATFORM_NAME, "LLMOps Platform"))
                .supportEmail(getValue(settings, SettingKey.PLATFORM_SUPPORT_EMAIL, null))
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Dynamic JavaMailSender (used by SmtpEmailService at send time)
    // ──────────────────────────────────────────────────────────────────────────

    public JavaMailSenderImpl buildMailSender(UUID orgId) {
        Map<String, AdminSetting> settings = loadSettingMap(orgId, "email.");

        String host = getValue(settings, SettingKey.EMAIL_SMTP_HOST, defaultSmtpHost);
        int port = getIntValue(settings, SettingKey.EMAIL_SMTP_PORT, defaultSmtpPort);
        String username = getValue(settings, SettingKey.EMAIL_SMTP_USERNAME, defaultSmtpUsername);
        boolean tls = getBoolValue(settings, SettingKey.EMAIL_ENABLE_TLS, true);
        boolean ssl = getBoolValue(settings, SettingKey.EMAIL_ENABLE_SSL, false);
        int timeout = getIntValue(settings, SettingKey.EMAIL_CONNECTION_TIMEOUT_MS, 5000);

        // Decrypt password
        AdminSetting passwordSetting = settings.get(SettingKey.EMAIL_SMTP_PASSWORD.getKey());
        String password;
        if (passwordSetting != null && passwordSetting.getEncryptedValue() != null) {
            password = encryptionService.decrypt(passwordSetting.getEncryptedValue());
        } else {
            password = defaultSmtpPassword;
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        if (username != null && !username.isBlank()) {
            sender.setUsername(username);
        }
        if (password != null && !password.isBlank()) {
            sender.setPassword(password);
        }

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", (username != null && !username.isBlank()) ? "true" : "false");
        props.put("mail.smtp.starttls.enable", String.valueOf(tls));
        props.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        props.put("mail.smtp.connectiontimeout", String.valueOf(timeout));
        props.put("mail.smtp.timeout", String.valueOf(timeout));
        props.put("mail.smtp.writetimeout", String.valueOf(timeout));

        return sender;
    }

    /**
     * Returns true if the organization has email sending enabled and configured.
     */
    @Transactional(readOnly = true)
    public boolean isEmailEnabled(UUID orgId) {
        Map<String, AdminSetting> settings = loadSettingMap(orgId, "email.");
        if (!getBoolValue(settings, SettingKey.EMAIL_ENABLED, false)) return false;
        String host = getValue(settings, SettingKey.EMAIL_SMTP_HOST, null);
        return host != null && !host.isBlank();
    }

    /**
     * Get resolved from-address for the organization.
     */
    @Transactional(readOnly = true)
    public String getFromAddress(UUID orgId) {
        Map<String, AdminSetting> settings = loadSettingMap(orgId, "email.");
        return getValue(settings, SettingKey.EMAIL_FROM_ADDRESS, defaultFromAddress);
    }

    /**
     * Get resolved from-name for the organization.
     */
    @Transactional(readOnly = true)
    public String getFromName(UUID orgId) {
        Map<String, AdminSetting> settings = loadSettingMap(orgId, "email.");
        return getValue(settings, SettingKey.EMAIL_FROM_NAME, defaultFromName);
    }

    /**
     * Get resolved base-url for the organization.
     */
    @Transactional(readOnly = true)
    public String getBaseUrl(UUID orgId) {
        Map<String, AdminSetting> settings = loadSettingMap(orgId, "platform.");
        return getValue(settings, SettingKey.PLATFORM_BASE_URL, defaultBaseUrl);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────────

    private void upsert(UUID orgId, SettingKey key, String value, boolean encrypted, User updatedBy) {
        Optional<AdminSetting> existing = settingRepository
                .findByOrganizationIdAndSettingKey(orgId, key.getKey());

        AdminSetting setting = existing.orElseGet(() -> {
            Organization org = new Organization();
            org.setId(orgId);
            return AdminSetting.builder()
                    .organization(org)
                    .settingKey(key.getKey())
                    .description(key.getDescription())
                    .isEncrypted(false)
                    .build();
        });

        setting.setSettingValue(value);
        setting.setIsEncrypted(false);
        setting.setUpdatedBy(updatedBy);
        settingRepository.save(setting);
    }

    private void upsertEncrypted(UUID orgId, SettingKey key, String plainValue, User updatedBy) {
        Optional<AdminSetting> existing = settingRepository
                .findByOrganizationIdAndSettingKey(orgId, key.getKey());

        AdminSetting setting = existing.orElseGet(() -> {
            Organization org = new Organization();
            org.setId(orgId);
            return AdminSetting.builder()
                    .organization(org)
                    .settingKey(key.getKey())
                    .description(key.getDescription())
                    .isEncrypted(true)
                    .build();
        });

        setting.setSettingValue(null);
        setting.setEncryptedValue(encryptionService.encrypt(plainValue));
        setting.setIsEncrypted(true);
        setting.setUpdatedBy(updatedBy);
        settingRepository.save(setting);
    }

    private Map<String, AdminSetting> loadSettingMap(UUID orgId, String prefix) {
        return settingRepository.findByOrganizationIdAndKeyPrefix(orgId, prefix)
                .stream()
                .collect(Collectors.toMap(AdminSetting::getSettingKey, s -> s));
    }

    private String getValue(Map<String, AdminSetting> map, SettingKey key, String defaultValue) {
        AdminSetting s = map.get(key.getKey());
        if (s == null) return defaultValue;
        return s.getSettingValue() != null ? s.getSettingValue() : defaultValue;
    }

    private int getIntValue(Map<String, AdminSetting> map, SettingKey key, int defaultValue) {
        String val = getValue(map, key, null);
        if (val == null) return defaultValue;
        try { return Integer.parseInt(val.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }

    private boolean getBoolValue(Map<String, AdminSetting> map, SettingKey key, boolean defaultValue) {
        String val = getValue(map, key, null);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val.trim());
    }

    private String buildTestEmailBody(String platformName) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #333; max-width: 500px; margin: 0 auto; padding: 20px;">
              <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 24px; border-radius: 8px 8px 0 0; text-align: center;">
                <h2 style="color: white; margin: 0;">%s</h2>
              </div>
              <div style="background: #fff; border: 1px solid #e0e0e0; border-top: none; border-radius: 0 0 8px 8px; padding: 24px;">
                <h3 style="color: #333;">Email Configuration Test</h3>
                <p>This is a test email to confirm your SMTP settings are working correctly.</p>
                <div style="background: #f0fdf4; border: 1px solid #86efac; border-radius: 6px; padding: 12px; margin: 16px 0;">
                  <strong style="color: #166534;">Your email configuration is working!</strong>
                </div>
                <p style="color: #666; font-size: 13px;">If you received this email, your SMTP settings are correctly configured in the LLMOps admin panel.</p>
              </div>
            </body>
            </html>
            """.formatted(platformName);
    }
}
