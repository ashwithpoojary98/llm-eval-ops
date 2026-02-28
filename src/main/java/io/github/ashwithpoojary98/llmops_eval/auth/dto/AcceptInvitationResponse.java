package io.github.ashwithpoojary98.llmops_eval.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptInvitationResponse {

    private String userId;
    private String email;
    private String fullName;
    private String role;
    private String organizationId;
    private String organizationName;
}
