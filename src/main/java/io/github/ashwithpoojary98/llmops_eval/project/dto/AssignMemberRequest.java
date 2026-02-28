package io.github.ashwithpoojary98.llmops_eval.project.dto;

import io.github.ashwithpoojary98.llmops_eval.project.entity.AccessLevel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignMemberRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @Builder.Default
    private AccessLevel accessLevel = AccessLevel.READ;
}
