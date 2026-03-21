package io.github.ashwithpoojary98.llmops_eval.settings.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestEmailResponse {
    private boolean success;
    private String message;
    private String errorDetail;
    private long latencyMs;
}
