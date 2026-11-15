package com.aiscalendar.ai.dto;

/**
 * Result of one assistant turn.
 *
 * <ul>
 *   <li>{@code status=need_info} — show {@link #assistantMessage} in chat; {@link #draft} is null</li>
 *   <li>{@code status=ready} — show message and open the event modal with {@link #draft}</li>
 * </ul>
 */
public record AssistantResponse(String status, String assistantMessage, EventDraftDto draft) {

  public static AssistantResponse needInfo(String message) {
    return new AssistantResponse("need_info", message, null);
  }

  public static AssistantResponse ready(String message, EventDraftDto draft) {
    return new AssistantResponse("ready", message, draft);
  }
}
