package io.github.ashwithpoojary98.llmops_eval.team.dto;

import io.github.ashwithpoojary98.llmops_eval.team.entity.TeamMemberRole;
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
public class AddTeamMemberRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @Builder.Default
    private TeamMemberRole role = TeamMemberRole.MEMBER;
}
