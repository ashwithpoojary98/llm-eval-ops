package io.github.ashwithpoojary98.llmops_eval.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmailConfigRequest {

    @NotBlank(message = "SMTP host is required")
    private String smtpHost;

    @NotNull(message = "SMTP port is required")
    @Min(value = 1, message = "Port must be between 1 and 65535")
    @Max(value = 65535, message = "Port must be between 1 and 65535")
    private Integer smtpPort;

    /** Leave blank to keep existing password */
    private String smtpUsername;

    /** Leave blank to keep existing password unchanged */
    private String smtpPassword;

    @NotBlank(message = "From email address is required")
    @Email(message = "From address must be a valid email")
    private String fromAddress;

    @NotBlank(message = "From name is required")
    private String fromName;

    private boolean enableTls = true;
    private boolean enableSsl = false;
    private int connectionTimeoutMs = 5000;
}
