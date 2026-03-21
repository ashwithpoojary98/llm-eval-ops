package io.github.ashwithpoojary98.llmops_eval.settings.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * All recognized setting keys with metadata (whether the value is sensitive).
 */
@Getter
@RequiredArgsConstructor
public enum SettingKey {

    // ── Email / SMTP ──────────────────────────────────────────────────────────
    EMAIL_SMTP_HOST("email.smtp.host", false, "SMTP server hostname"),
    EMAIL_SMTP_PORT("email.smtp.port", false, "SMTP server port (e.g. 587, 465, 25)"),
    EMAIL_SMTP_USERNAME("email.smtp.username", false, "SMTP authentication username"),
    EMAIL_SMTP_PASSWORD("email.smtp.password", true, "SMTP authentication password"),
    EMAIL_FROM_ADDRESS("email.from.address", false, "Sender email address"),
    EMAIL_FROM_NAME("email.from.name", false, "Sender display name"),
    EMAIL_ENABLE_TLS("email.enable.tls", false, "Enable STARTTLS (true/false)"),
    EMAIL_ENABLE_SSL("email.enable.ssl", false, "Use SSL/TLS on connect (true/false)"),
    EMAIL_CONNECTION_TIMEOUT_MS("email.connection.timeout.ms", false, "Connection timeout in milliseconds"),
    EMAIL_ENABLED("email.enabled", false, "Master switch: enable/disable email sending (true/false)"),

    // ── Platform ──────────────────────────────────────────────────────────────
    PLATFORM_BASE_URL("platform.base.url", false, "Frontend base URL (used in email links)"),
    PLATFORM_NAME("platform.name", false, "Platform display name shown in emails"),
    PLATFORM_SUPPORT_EMAIL("platform.support.email", false, "Support contact email shown in emails");

    private final String key;
    private final boolean sensitive;
    private final String description;

    public static SettingKey fromKey(String key) {
        for (SettingKey sk : values()) {
            if (sk.key.equals(key)) return sk;
        }
        throw new IllegalArgumentException("Unknown setting key: " + key);
    }
}
