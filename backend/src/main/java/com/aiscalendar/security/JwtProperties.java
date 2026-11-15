package com.aiscalendar.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** JWT settings from {@code app.jwt.*} (secret must be ≥32 chars for HMAC-SHA). */
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

  private String secret;
  /** Token lifetime in milliseconds (default 24h via application.yml). */
  private long expirationMs;

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public long getExpirationMs() {
    return expirationMs;
  }

  public void setExpirationMs(long expirationMs) {
    this.expirationMs = expirationMs;
  }
}
