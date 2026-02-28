package io.github.ashwithpoojary98.llmops_eval.common.exception;

import io.github.ashwithpoojary98.llmops_eval.auth.exception.*;
import io.github.ashwithpoojary98.llmops_eval.auth.model.APIResponse;
import io.github.ashwithpoojary98.llmops_eval.dataset.exception.DatasetNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.evaluation.exception.EvaluationNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.evaluation.exception.EvaluationValidationException;
import io.github.ashwithpoojary98.llmops_eval.llms.exception.LlmEndpointNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.project.exception.ProjectNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.team.exception.TeamMemberNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.team.exception.TeamNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.testcases.exception.TestCaseNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<APIResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(APIResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .errorCode("AUTHENTICATION_FAILED")
                        .error(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<APIResponse<Void>> handleInvalidTokenException(InvalidTokenException ex) {
        log.warn("Invalid token: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(APIResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .errorCode("INVALID_TOKEN")
                        .error(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            errors.put(fieldName, error.getDefaultMessage());
        });

        return ResponseEntity.badRequest()
                .body(APIResponse.<Map<String, String>>builder()
                        .success(false)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .errorCode("VALIDATION_ERROR")
                        .error("Validation failed")
                        .data(errors)
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<APIResponse<Void>> handleValidationException(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        return ResponseEntity.badRequest()
                .body(APIResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .errorCode("VALIDATION_ERROR")
                        .error(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<APIResponse<Void>> handleUserNotFoundException(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(APIResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .errorCode("USER_NOT_FOUND")
                        .error(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(TeamNotFoundException.class)
    public ResponseEntity<APIResponse<Void>> handleTeamNotFoundException(TeamNotFoundException ex) {
        log.warn("Team not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(APIResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .errorCode("TEAM_NOT_FOUND")
                        .error(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(TeamMemberNotFoundException.class)
    public ResponseEntity<APIResponse<Void>> handleTeamMemberNotFoundException(TeamMemberNotFoundException ex) {
        log.warn("Team member not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(APIResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .errorCode("TEAM_MEMBER_NOT_FOUND")
                        .error(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<APIResponse<Void>> handleProjectNotFoundException(ProjectNotFoundException ex) {
        log.warn("Project not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(APIResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .errorCode("PROJECT_NOT_FOUND")
                        .error(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(TestCaseNotFoundException.class)
    public ResponseEntity<APIResponse<Void>> handleTestCaseNotFoundException(TestCaseNotFoundException ex) {
        log.warn("Test case not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(APIResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .errorCode("TEST_CASE_NOT_FOUND")
                        .error(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(DatasetNotFoundException.class)
    public ResponseEntity<APIResponse<Void>> handleDatasetNotFoundException(DatasetNotFoundException ex) {
        log.warn("Dataset not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(APIResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .errorCode("DATASET_NOT_FOUND")
                        .error(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(LlmEndpointNotFoundException.class)
    public ResponseEntity<APIResponse<Void>> handleLlmEndpointNotFoundException(LlmEndpointNotFoundException ex) {
        log.warn("LLM endpoint not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(APIResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .errorCode("LLM_ENDPOINT_NOT_FOUND")
                        .error(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(EvaluationNotFoundException.class)
    public ResponseEntity<APIResponse<Void>> handleEvaluationNotFoundException(EvaluationNotFoundException ex) {
        log.warn("Evaluation not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(APIResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .errorCode("EVALUATION_NOT_FOUND")
                        .error(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(EvaluationValidationException.class)
    public ResponseEntity<APIResponse<Void>> handleEvaluationValidationException(EvaluationValidationException ex) {
        log.warn("Evaluation validation error: {}", ex.getMessage());

        return ResponseEntity.badRequest()
                .body(APIResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .errorCode("EVALUATION_VALIDATION_ERROR")
                        .error(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<APIResponse<Void>> handleConflictException(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(APIResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.CONFLICT.value())
                        .errorCode("CONFLICT")
                        .error(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler({ForbiddenException.class, DomainNotAllowedException.class})
    public ResponseEntity<APIResponse<Void>> handleForbiddenException(RuntimeException ex) {
        log.warn("Forbidden: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(APIResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.FORBIDDEN.value())
                        .errorCode("FORBIDDEN")
                        .error(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(APIResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.FORBIDDEN.value())
                        .errorCode("ACCESS_DENIED")
                        .error("You don't have permission to access this resource")
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<APIResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("Bad credentials: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(APIResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .errorCode("BAD_CREDENTIALS")
                        .error(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(APIResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .errorCode("INTERNAL_ERROR")
                        .error("An unexpected error occurred")
                        .timestamp(Instant.now())
                        .build());
    }
}

