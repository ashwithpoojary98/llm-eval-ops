package io.github.ashwithpoojary98.llmops_eval.team.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberResponse {

    private String id;
    private String userId;
    private String email;
    private String fullName;
    private String role;
    private String addedById;
    private String addedByName;
    private Instant joinedAt;
}
