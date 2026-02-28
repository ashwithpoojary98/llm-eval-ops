package io.github.ashwithpoojary98.llmops_eval.auth.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDeactivatedEvent {

    private final String email;
    private final String fullName;
    private final String organizationName;
}
