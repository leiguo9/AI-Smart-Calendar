package com.aiscalendar.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed config for OpenAI, bound from {@code app.openai.*}.
 *
 * <p>Example mapping:
 * <ul>
 *   <li>{@code OPENAI_API_KEY} → {@code app.openai.api-key} → {@link #apiKey}</li>
 *   <li>{@code OPENAI_MODEL} → {@code app.openai.model}</li>
 * </ul>
 * Spring Boot "relaxed binding" converts env vars / kebab-case YAML into these Java fields.
 */
@ConfigurationProperties(prefix = "app.openai")
public class OpenAiProperties {

  private String apiKey = "";
  private String model = "gpt-4o-mini";
  /** Overrideable for proxies or Azure-compatible endpoints. */
  private String baseUrl = "https://api.openai.com/v1";

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }
}
