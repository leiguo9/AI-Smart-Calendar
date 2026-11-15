package com.aiscalendar.ai.dto;

import com.aiscalendar.event.EventCategory;
import java.time.Instant;

/**
 * Proposed event fields from the model — same shape as create-event, plus recurrence metadata.
 *
 * <p>Not persisted. The frontend copies these into {@code EventModal}; saving still goes through
 * {@code POST /api/events}.
 */
public record EventDraftDto(
    String title,
    String description,
    Instant startAt,
    Instant endAt,
    boolean allDay,
    EventCategory category,
    /** True if the user implied a series (daily/weekly/…). */
    boolean recurrenceDetected,
    /** Human-readable note when recurrence is detected; otherwise null. */
    String warning) {}
