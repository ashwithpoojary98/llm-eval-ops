package io.github.ashwithpoojary98.llmops_eval.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private String id;
    private String name;
    private String description;
    private String status;
    private String createdById;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ProjectTeamResponse> teams;
    private List<ProjectMemberResponse> members;
    private String myAccessLevel; // Current user's access level
}
