package com.aiscalendar.common;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Central place that maps exceptions → HTTP responses for all controllers.
 *
 * <p>Keeps controllers thin: they throw (or let validation fail) instead of building error JSON.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String, String>> handleApi(ApiException ex) {
    return ResponseEntity.status(ex.getStatus()).body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
    // Generic message on purpose — don't reveal whether email or password was wrong.
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", "Invalid email or password"));
  }

  /** Triggered when {@code @Valid} fails on a request body (e.g. blank title). */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
    String details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
    return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "details", details));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
    // Catch-all: log-worthy server bugs should not dump stack traces to the browser.
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("error", "Unexpected server error"));
  }
}
