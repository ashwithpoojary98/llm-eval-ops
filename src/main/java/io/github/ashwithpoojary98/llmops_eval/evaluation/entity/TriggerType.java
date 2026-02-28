package io.github.ashwithpoojary98.llmops_eval.evaluation.entity;

/**
 * How an evaluation run was triggered.
 */
public enum TriggerType {
    MANUAL,
    CI_CD,
    SCHEDULED
}
