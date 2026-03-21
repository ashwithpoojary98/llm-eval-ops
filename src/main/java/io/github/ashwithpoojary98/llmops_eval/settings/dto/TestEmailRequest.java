package io.github.ashwithpoojary98.llmops_eval.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TestEmailRequest {

    @NotBlank
    @Email(message = "Must be a valid email address")
    private String recipientEmail;
}
