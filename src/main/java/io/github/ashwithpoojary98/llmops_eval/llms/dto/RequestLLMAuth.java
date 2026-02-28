package io.github.ashwithpoojary98.llmops_eval.llms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestLLMAuth {
    private AuthType type;
    private String headerName;
    private String token;
    private String username;
    private String password;
}
