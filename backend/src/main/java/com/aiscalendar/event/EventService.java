package com.aiscalendar.event;

import com.aiscalendar.common.ApiException;
import com.aiscalendar.event.dto.EventRequest;
import com.aiscalendar.event.dto.EventResponse;
import com.aiscalendar.user.User;
import com.aiscalendar.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event business rules and persistence.
 *
 * <p>Ownership is enforced here: updates/deletes use {@code findByIdAndOwnerId}, so user A cannot
 * mutate user B's events even if they guess a UUID.
 */
@Service
public class EventService {

  private final CalendarEventRepository eventRepository;
  private final UserRepository userRepository;

  public EventService(CalendarEventRepository eventRepository, UserRepository userRepository) {
    this.eventRepository = eventRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<EventResponse> list(UUID ownerId, Instant from, Instant to) {
    if (!to.isAfter(from)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "to must be after from");
    }
    return eventRepository.findOverlapping(ownerId, from, to).stream()
        .map(EventResponse::from)
        .toList();
  }

  @Transactional
  public EventResponse create(UUID ownerId, EventRequest request) {
    validateRange(request);
    User owner =
        userRepository
            .findById(ownerId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    CalendarEvent event =
        new CalendarEvent(
            owner,
            request.title().trim(),
            normalizeDescription(request.description()),
            request.startAt(),
            request.endAt(),
            request.allDay(),
            normalizeCategory(request.category()));
    return EventResponse.from(eventRepository.save(event));
  }

  @Transactional
  public EventResponse update(UUID ownerId, UUID eventId, EventRequest request) {
    validateRange(request);
    CalendarEvent event =
        eventRepository
            .findByIdAndOwnerId(eventId, ownerId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
    // Entity is managed inside the transaction — mutating fields is enough; Hibernate flushes.
    event.update(
        request.title().trim(),
        normalizeDescription(request.description()),
        request.startAt(),
        request.endAt(),
        request.allDay(),
        normalizeCategory(request.category()));
    return EventResponse.from(event);
  }

  @Transactional
  public void delete(UUID ownerId, UUID eventId) {
    CalendarEvent event =
        eventRepository
            .findByIdAndOwnerId(eventId, ownerId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
    eventRepository.delete(event);
  }

  private void validateRange(EventRequest request) {
    if (!request.endAt().isAfter(request.startAt())) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "endAt must be after startAt");
    }
  }

  private String normalizeDescription(String description) {
    if (description == null || description.isBlank()) {
      return null;
    }
    return description.trim();
  }

  private EventCategory normalizeCategory(EventCategory category) {
    return category == null ? EventCategory.OTHER : category;
  }
}
