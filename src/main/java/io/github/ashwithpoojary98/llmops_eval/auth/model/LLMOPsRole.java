package io.github.ashwithpoojary98.llmops_eval.auth.model;

public enum LLMOPsRole {
    GUEST(0),
    VIEWER(10),
    DEVELOPER(20),
    EVALUATOR(30),
    ADMIN(100);

    private final int priority;

    LLMOPsRole(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public boolean canInvite(LLMOPsRole targetRole) {
        return this == ADMIN && this.priority >= targetRole.priority;
    }

    public boolean canManageUsers() {
        return this == ADMIN;
    }

    public boolean isAtLeast(LLMOPsRole role) {
        return this.priority >= role.priority;
    }
}
