package com.aiscalendar.auth.dto;

import java.util.UUID;

/**
 * Returned by signup/login. Frontend stores {@link #token} and uses it as Bearer auth.
 */
public record AuthResponse(String token, UUID userId, String email, String displayName) {}
