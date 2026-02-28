package io.github.ashwithpoojary98.llmops_eval.testcases.exception;

public class TestCaseNotFoundException extends RuntimeException {

    public TestCaseNotFoundException(String message) {
        super(message);
    }
}
