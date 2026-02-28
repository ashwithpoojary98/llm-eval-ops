package io.github.ashwithpoojary98.llmops_eval.testcases.dto;

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
public class TestCaseResponse {

    private String id;
    private String projectId;
    private String projectName;
    private String question;
    private String llmOutput;
    private List<String> retrievedContext;
    private String groundTruth;
    private Boolean isActive;
    private String createdById;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;
}
