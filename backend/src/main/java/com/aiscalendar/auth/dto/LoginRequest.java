package com.aiscalendar.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Login body. Password max 72 matches BCrypt's practical input limit. */
public record LoginRequest(
    @NotBlank @Email String email, @NotBlank @Size(min = 8, max = 72) String password) {}
