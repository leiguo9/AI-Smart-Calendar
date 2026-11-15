package com.aiscalendar.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * One chat bubble in the assistant thread.
 *
 * <p>Only {@code user} and {@code assistant} are allowed — we don't accept {@code system} from
 * clients (the server injects its own system prompt).
 */
public record ChatMessageDto(
    @NotBlank
        @Pattern(regexp = "user|assistant", message = "role must be user or assistant")
        String role,
    @NotBlank @Size(max = 500) String content) {}
