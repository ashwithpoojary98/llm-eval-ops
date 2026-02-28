package io.github.ashwithpoojary98.llmops_eval.dataset.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveTestCasesRequest {

    @NotEmpty(message = "At least one test case ID is required")
    private List<UUID> testCaseIds;
}
