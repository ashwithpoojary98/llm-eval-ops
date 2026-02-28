package io.github.ashwithpoojary98.llmops_eval.evaluation.entity;

/**
 * Evaluation execution modes.
 */
public enum EvaluationMode {
    /**
     * LLM output already exists in test cases.
     */
    PRE_GENERATED,

    /**
     * Generate output using target LLM during evaluation.
     */
    LIVE_GENERATION
}
