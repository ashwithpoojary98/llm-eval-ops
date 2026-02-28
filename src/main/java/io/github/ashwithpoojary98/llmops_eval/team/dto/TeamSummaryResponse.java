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
public class TeamSummaryResponse {

    private String id;
    private String name;
    private String description;
    private int memberCount;
    private Instant createdAt;
}
