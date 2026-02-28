package io.github.ashwithpoojary98.llmops_eval.auth.dto;

import io.github.ashwithpoojary98.llmops_eval.auth.model.LLMOPsRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRoleRequest {

    @NotNull(message = "Role is required")
    private LLMOPsRole role;
}
