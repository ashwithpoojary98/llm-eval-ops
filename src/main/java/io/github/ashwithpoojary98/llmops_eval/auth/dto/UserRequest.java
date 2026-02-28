package io.github.ashwithpoojary98.llmops_eval.auth.dto;

import lombok.Data;

@Data
public class UserRequest {
    private String email;
    private String password;
    private Boolean isSSOEnabled;
}
