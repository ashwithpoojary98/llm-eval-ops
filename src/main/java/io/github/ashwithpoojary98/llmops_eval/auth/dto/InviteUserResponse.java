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
public class InviteUserResponse {

    private String invitationId;
    private String email;
    private String role;
    private String invitationToken;  // Only returned on creation
    private Instant expiresAt;
    private String invitedBy;
    private String organizationName;
}
