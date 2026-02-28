package io.github.ashwithpoojary98.llmops_eval.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Integer expiresIn;  // seconds

    // User info
    private String userId;
    private String email;
    private String fullName;
    private String role;

    // Organization info
    private String organizationId;
    private String organizationName;

    // Flags
    private Boolean mustChangePassword;
}
