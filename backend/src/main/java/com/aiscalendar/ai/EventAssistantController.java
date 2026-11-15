package com.aiscalendar.ai;

import com.aiscalendar.ai.dto.AssistantRequest;
import com.aiscalendar.ai.dto.AssistantResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry point for the conversational assistant.
 *
 * <p>Lives under {@code /api/events} next to CRUD, but this endpoint never saves an event —
 * it only clarifies or proposes a draft. JWT auth is required because {@link
 * com.aiscalendar.security.SecurityConfig} protects everything except {@code /api/auth/**}.
 */
@RestController
@RequestMapping("/api/events")
public class EventAssistantController {

  private final OpenAiEventAssistant eventAssistant;

  public EventAssistantController(OpenAiEventAssistant eventAssistant) {
    this.eventAssistant = eventAssistant;
  }

  /** One chat turn: send history + time zone, get {@code need_info} or {@code ready}. */
  @PostMapping("/assistant")
  public AssistantResponse assistant(@Valid @RequestBody AssistantRequest request) {
    return eventAssistant.turn(request.messages(), request.timeZone());
  }
}
