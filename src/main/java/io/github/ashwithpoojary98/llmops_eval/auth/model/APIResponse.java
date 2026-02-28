package io.github.ashwithpoojary98.llmops_eval.auth.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class APIResponse<T> {
    private boolean success;
    private int statusCode;
    private String message;
    private String error;
    private String errorCode;
    private String traceId;
    private Instant timestamp;
    private T data;
}

