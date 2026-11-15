package com.aiscalendar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point.
 *
 * <p>{@link SpringBootApplication} turns on component scanning for this package and below
 * ({@code auth}, {@code event}, {@code ai}, {@code security}, …) and auto-configures web, JPA,
 * security, etc. based on the classpath.
 */
@SpringBootApplication
public class AiSmartCalendarApplication {

  public static void main(String[] args) {
    SpringApplication.run(AiSmartCalendarApplication.class, args);
  }
}
