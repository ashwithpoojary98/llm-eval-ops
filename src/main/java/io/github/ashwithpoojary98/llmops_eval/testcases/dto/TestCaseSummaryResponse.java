package io.github.ashwithpoojary98.llmops_eval.testcases.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseSummaryResponse {

    private String id;
    private String projectId;
    private String question;
    private String groundTruth;
    private Boolean isActive;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;
}
