package io.github.ashwithpoojary98.llmops_eval.llms.exception;

public class LlmEndpointNotFoundException extends RuntimeException {

    public LlmEndpointNotFoundException(String message) {
        super(message);
    }
}
