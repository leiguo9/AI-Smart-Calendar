package com.aiscalendar.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wires AI-related Spring beans.
 *
 * <p>{@link EnableConfigurationProperties} tells Spring to bind {@code app.openai.*} from
 * {@code application.yml} / env vars into {@link OpenAiProperties}.
 */
@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class AiConfig {}
