package io.github.ashwithpoojary98.llmops_eval.llms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterLLMRequest {
    private String llmName;
    private String llmId;
    private String llmURL;
    private String llmURLType;
    private RequestLLMAuth auth;
}
