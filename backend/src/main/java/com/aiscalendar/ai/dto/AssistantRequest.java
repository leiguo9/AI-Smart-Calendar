package com.aiscalendar.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Body for {@code POST /api/events/assistant}.
 *
 * <p>The client owns conversation state: it resends the whole thread each turn (capped at 20
 * messages so prompts stay bounded).
 */
public record AssistantRequest(
    @NotEmpty @Size(max = 20) List<@Valid ChatMessageDto> messages,
    /** IANA zone from the browser, e.g. {@code America/Los_Angeles}. Optional → treated as UTC. */
    @Size(max = 64) String timeZone) {}
