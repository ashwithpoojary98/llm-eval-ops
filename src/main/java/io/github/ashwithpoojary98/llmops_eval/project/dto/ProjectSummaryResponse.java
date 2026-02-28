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
public class ProjectSummaryResponse {

    private String id;
    private String name;
    private String description;
    private String status;
    private int teamCount;
    private int memberCount;
    private Instant createdAt;
    private String myAccessLevel;
}
