package io.github.ashwithpoojary98.llmops_eval.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTeamResponse {

    private String id;
    private String teamId;
    private String teamName;
    private String accessLevel;
    private String assignedById;
    private String assignedByName;
    private Instant assignedAt;
}
