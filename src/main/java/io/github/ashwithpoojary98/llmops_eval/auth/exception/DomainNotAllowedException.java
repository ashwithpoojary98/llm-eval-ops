package io.github.ashwithpoojary98.llmops_eval.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class DomainNotAllowedException extends RuntimeException {

    public DomainNotAllowedException(String message) {
        super(message);
    }
}
