package io.github.ashwithpoojary98.llmops_eval.testcases.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTestCaseRequest {

    @NotBlank(message = "Question is required")
    @Size(max = 10000, message = "Question cannot exceed 10000 characters")
    private String question;

    @Size(max = 50000, message = "LLM output cannot exceed 50000 characters")
    private String llmOutput;

    private List<String> retrievedContext;

    @Size(max = 50000, message = "Ground truth cannot exceed 50000 characters")
    private String groundTruth;
}
