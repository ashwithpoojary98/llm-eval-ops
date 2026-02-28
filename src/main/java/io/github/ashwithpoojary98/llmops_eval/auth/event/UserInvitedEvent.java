package io.github.ashwithpoojary98.llmops_eval.auth.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class UserInvitedEvent {

    private final String email;
    private final String role;
    private final String invitationToken;
    private final Instant expiresAt;
    private final String invitedByName;
    private final String organizationName;
}
