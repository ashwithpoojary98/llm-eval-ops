package io.github.ashwithpoojary98.llmops_eval.webhook.entity;

/**
 * All event types that can be emitted to webhook subscribers.
 */
public enum WebhookEvent {

    // LLM health events
    LLM_HEALTH_DOWN,
    LLM_HEALTH_RECOVERED,
    LLM_HEALTH_DEGRADED,

    // Evaluation lifecycle events
    EVALUATION_STARTED,
    EVALUATION_COMPLETED,
    EVALUATION_FAILED,
    EVALUATION_SCORE_BELOW_THRESHOLD,

    // Dataset events
    DATASET_IMPORTED,
    DATASET_DELETED,

    // Project events
    PROJECT_MEMBER_ADDED,
    PROJECT_MEMBER_REMOVED
}
