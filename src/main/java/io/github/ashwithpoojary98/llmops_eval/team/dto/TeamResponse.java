package io.github.ashwithpoojary98.llmops_eval.team.dto;

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
public class TeamResponse {

    private String id;
    private String name;
    private String description;
    private int memberCount;
    private String createdById;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;
    private List<TeamMemberResponse> members;
}
