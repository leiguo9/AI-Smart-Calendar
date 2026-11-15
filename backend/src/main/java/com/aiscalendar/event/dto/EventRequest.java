package com.aiscalendar.event.dto;

import com.aiscalendar.event.EventCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Create/update body for {@code POST/PUT /api/events}. Validated by {@code @Valid}. */
public record EventRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @NotNull Instant startAt,
    @NotNull Instant endAt,
    boolean allDay,
    EventCategory category) {}
