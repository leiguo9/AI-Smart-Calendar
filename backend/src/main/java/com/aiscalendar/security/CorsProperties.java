package com.aiscalendar.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Browser origins allowed to call the API (comma-separated in config / env).
 *
 * <p>Example: {@code APP_CORS_ORIGINS=http://localhost:5173,http://localhost:3000}
 */
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

  private String allowedOrigins = "http://localhost:5173";

  public List<String> getAllowedOrigins() {
    // Split once here so SecurityConfig can pass a clean List to Spring CORS.
    return Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  public void setAllowedOrigins(String allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }
}
