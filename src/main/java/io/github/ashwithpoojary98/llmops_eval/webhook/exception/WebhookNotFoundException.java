package io.github.ashwithpoojary98.llmops_eval.webhook.exception;

public class WebhookNotFoundException extends RuntimeException {
    public WebhookNotFoundException(String message) {
        super(message);
    }
}
