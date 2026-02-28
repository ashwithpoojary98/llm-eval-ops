package io.github.ashwithpoojary98.llmops_eval.project.entity;

import lombok.Getter;

@Getter
public enum AccessLevel {
    ADMIN(3),  // Full control
    WRITE(2),  // Can create/edit
    READ(1);   // View only

    private final int priority;

    AccessLevel(int priority) {
        this.priority = priority;
    }


    public boolean canWrite() {
        return this == ADMIN || this == WRITE;
    }

    public boolean canAdmin() {
        return this == ADMIN;
    }
}
