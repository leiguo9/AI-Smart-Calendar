package com.aiscalendar.common;

import org.springframework.http.HttpStatus;

/**
 * Domain/HTTP error we throw from services.
 *
 * <p>Carries an {@link HttpStatus} so {@link GlobalExceptionHandler} can turn it into a consistent
 * JSON {@code { "error": "…" }} response without try/catch in every controller.
 */
public class ApiException extends RuntimeException {

  private final HttpStatus status;

  public ApiException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatus getStatus() {
    return status;
  }
}
