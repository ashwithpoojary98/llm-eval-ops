package io.github.ashwithpoojary98.llmops_eval.llms.entity;

/**
 * Defines the purpose/role of an LLM endpoint.
 */
public enum LlmType {
    /**
     * Client LLM - The LLM being tested.
     * Receives questions and generates responses during evaluation.
     */
    CLIENT,

    /**
     * Judge LLM - The LLM that evaluates responses.
     * Used for LLM-as-Judge metrics like Faithfulness, Relevance, etc.
     */
    JUDGE
}
