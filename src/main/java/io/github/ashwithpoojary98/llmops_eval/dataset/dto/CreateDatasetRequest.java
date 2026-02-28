package io.github.ashwithpoojary98.llmops_eval.dataset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class CreateDatasetRequest {

    @NotBlank(message = "Dataset name is required")
    @Size(min = 1, max = 255, message = "Dataset name must be between 1 and 255 characters")
    private String name;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    private List<UUID> testCaseIds;
}
