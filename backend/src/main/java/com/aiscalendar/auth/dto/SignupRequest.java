package com.aiscalendar.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Signup body — same password rules as login for consistency. */
public record SignupRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 1, max = 100) String displayName,
    @NotBlank @Size(min = 8, max = 72) String password) {}
