package com.aiscalendar.event.dto;

import com.aiscalendar.event.CalendarEvent;
import com.aiscalendar.event.EventCategory;
import java.time.Instant;
import java.util.UUID;

public record EventResponse(
    UUID id,
    String title,
    String description,
    Instant startAt,
    Instant endAt,
    boolean allDay,
    EventCategory category,
    Instant createdAt,
    Instant updatedAt) {

  public static EventResponse from(CalendarEvent event) {
    return new EventResponse(
        event.getId(),
        event.getTitle(),
        event.getDescription(),
        event.getStartAt(),
        event.getEndAt(),
        event.isAllDay(),
        event.getCategory() == null ? EventCategory.OTHER : event.getCategory(),
        event.getCreatedAt(),
        event.getUpdatedAt());
  }
}
