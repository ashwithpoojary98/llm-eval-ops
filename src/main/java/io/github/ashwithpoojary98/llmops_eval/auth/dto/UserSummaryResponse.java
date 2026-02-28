package io.github.ashwithpoojary98.llmops_eval.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSummaryResponse {

    private String id;
    private String email;
    private String fullName;
    private String role;
    private Boolean isActive;
    private Boolean isSsoUser;
    private Boolean emailVerified;
    private Instant lastLoginAt;
    private Instant createdAt;
}
