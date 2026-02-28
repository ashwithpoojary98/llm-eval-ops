package io.github.ashwithpoojary98.llmops_eval.team.dto;

import io.github.ashwithpoojary98.llmops_eval.team.entity.TeamMemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTeamMemberRequest {

    @NotNull(message = "Role is required")
    private TeamMemberRole role;
}
