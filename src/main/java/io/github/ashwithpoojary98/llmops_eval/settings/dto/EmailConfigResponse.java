package io.github.ashwithpoojary98.llmops_eval.settings.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailConfigResponse {
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    private boolean hasPassword;          // never return the actual password
    private String fromAddress;
    private String fromName;
    private boolean enableTls;
    private boolean enableSsl;
    private int connectionTimeoutMs;
    private boolean emailEnabled;
    private boolean configured;           // true if SMTP host has been set
    private Instant lastUpdatedAt;
}
