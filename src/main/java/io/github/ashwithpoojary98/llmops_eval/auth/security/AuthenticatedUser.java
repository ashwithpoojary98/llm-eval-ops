package io.github.ashwithpoojary98.llmops_eval.auth.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUser {

    private String userId;
    private String email;
    private String role;
    private String organizationId;

    public UUID getUserIdAsUUID() {
        return UUID.fromString(userId);
    }

    public UUID getOrganizationIdAsUUID() {
        return UUID.fromString(organizationId);
    }
}
