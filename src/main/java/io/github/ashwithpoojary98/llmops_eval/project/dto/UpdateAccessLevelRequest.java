package io.github.ashwithpoojary98.llmops_eval.project.dto;

import io.github.ashwithpoojary98.llmops_eval.project.entity.AccessLevel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccessLevelRequest {

    @NotNull(message = "Access level is required")
    private AccessLevel accessLevel;
}
