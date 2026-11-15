package com.aiscalendar.event;

import com.aiscalendar.event.dto.EventRequest;
import com.aiscalendar.event.dto.EventResponse;
import com.aiscalendar.security.UserPrincipal;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST CRUD for calendar events. Every method takes {@link UserPrincipal} so we always scope
 * data to the signed-in user (never trust a client-supplied owner id).
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

  private final EventService eventService;

  public EventController(EventService eventService) {
    this.eventService = eventService;
  }

  /** FullCalendar asks for events overlapping the visible range ({@code from}/{@code to}). */
  @GetMapping
  public List<EventResponse> list(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
    return eventService.list(principal.getId(), from, to);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EventResponse create(
      @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody EventRequest request) {
    return eventService.create(principal.getId(), request);
  }

  @PutMapping("/{id}")
  public EventResponse update(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody EventRequest request) {
    return eventService.update(principal.getId(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
    eventService.delete(principal.getId(), id);
  }
}
