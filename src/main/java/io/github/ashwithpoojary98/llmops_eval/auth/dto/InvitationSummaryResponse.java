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
public class InvitationSummaryResponse {

    private String id;
    private String email;
    private String role;
    private String status;  // PENDING, ACCEPTED, REVOKED, EXPIRED
    private String invitedBy;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant acceptedAt;
}
