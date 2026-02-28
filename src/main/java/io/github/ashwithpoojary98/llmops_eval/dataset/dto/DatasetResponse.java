package io.github.ashwithpoojary98.llmops_eval.dataset.dto;

import io.github.ashwithpoojary98.llmops_eval.testcases.dto.TestCaseSummaryResponse;
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
public class DatasetResponse {

    private String id;
    private String projectId;
    private String projectName;
    private String name;
    private String description;
    private Boolean isActive;
    private int testCaseCount;
    private List<TestCaseSummaryResponse> testCases;
    private String createdById;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;
}
