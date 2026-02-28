package io.github.ashwithpoojary98.llmops_eval.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordTokenValidationResponse {

    private boolean valid;
    private String email;
    private String message;
}
