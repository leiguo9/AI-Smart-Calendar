package com.aiscalendar.ai;

import com.aiscalendar.ai.dto.AssistantResponse;
import com.aiscalendar.ai.dto.ChatMessageDto;
import com.aiscalendar.ai.dto.EventDraftDto;
import com.aiscalendar.common.ApiException;
import com.aiscalendar.event.EventCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Conversational event helper backed by OpenAI Chat Completions + tool calling.
 *
 * <p>High-level flow for one "turn":
 * <ol>
 *   <li>Client sends the full chat history ({@code messages}) and the user's IANA time zone.</li>
 *   <li>We call OpenAI with two tools: {@code ask_clarification} and {@code propose_event}.</li>
 *   <li>The model must pick one tool (we set {@code tool_choice=required}).</li>
 *   <li>We map that tool call into {@link AssistantResponse}: either ask the user something,
 *       or return a draft the UI can preview in the event modal.</li>
 * </ol>
 *
 * <p>We do <em>not</em> persist chat on the server — the browser keeps history and resends it
 * each turn. We also do not run a multi-step tool loop here: one model reply → one mapped response.
 */
@Service
public class OpenAiEventAssistant {

  /** Shown in the UI when the model detects a repeating schedule we don't support yet. */
  private static final String RECURRENCE_WARNING =
      "Recurring events aren't supported yet — previewing the next occurrence only.";

  /**
   * Instructions the model always sees first. This is where we encode product rules:
   * ask instead of guessing times, default 1h duration, recurrence → next occurrence only.
   */
  private static final String SYSTEM_PROMPT =
      """
      You help create a single calendar event through conversation.
      You MUST respond by calling exactly one tool:
      - ask_clarification — when required details are missing or ambiguous \
        (especially date or clock time; also unclear title). Ask one concise question.
      - propose_event — only when you can determine start and end from what the user said \
        or agreed to. Do NOT invent a clock time the user did not provide or confirm. \
        If duration is omitted but start time is known, default to 1 hour. \
        For all-day events, startAt is local midnight as UTC Instant and endAt is the next \
        local midnight as UTC Instant. Resolve relative dates using the provided current time \
        and IANA time zone. Category must be WORK, PERSONAL, HEALTH, or OTHER. \
        If recurrence is implied, set recurrenceDetected true and return only the NEXT occurrence.

      Prefer ask_clarification over guessing times.
      """;

  private final OpenAiProperties properties;
  /** Jackson mapper — used to build the request JSON and parse the model response. */
  private final ObjectMapper objectMapper;
  /** JDK HTTP client (no extra OpenAI SDK dependency). */
  private final HttpClient httpClient;

  public OpenAiEventAssistant(OpenAiProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  }

  /**
   * Handle one user message in an ongoing conversation.
   *
   * @param messages full thread so far (roles {@code user}/{@code assistant}); last must be user
   * @param timeZone IANA id from the browser (e.g. {@code America/Los_Angeles}) so "tomorrow 3pm"
   *     resolves correctly
   */
  public AssistantResponse turn(List<ChatMessageDto> messages, String timeZone) {
    String apiKey = properties.getApiKey() == null ? "" : properties.getApiKey().trim();
    if (apiKey.isEmpty()) {
      // 503: the service is up, but this feature can't run without a key
      throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "OpenAI API key not configured");
    }
    if (messages == null || messages.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "messages must not be empty");
    }
    ChatMessageDto last = messages.get(messages.size() - 1);
    if (!"user".equals(last.role())) {
      // A turn always starts from new user input; otherwise we'd re-process an assistant reply.
      throw new ApiException(HttpStatus.BAD_REQUEST, "last message must be from the user");
    }

    ZoneId zone = resolveZone(timeZone);
    Instant now = Instant.now();
    // Fresh "now" each turn so relative phrases like "in 2 hours" stay accurate.
    String context =
        """
        Current time (UTC): %s
        User time zone: %s
        Current local time: %s
        """
            .formatted(
                now.toString(),
                zone.getId(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now.atZone(zone)));

    // OpenAI returns an assistant "message" object (may contain tool_calls instead of text).
    JsonNode message = callOpenAi(apiKey, context, messages);
    return mapToolResponse(message);
  }

  /**
   * POST to {@code /v1/chat/completions} with our tools registered.
   *
   * @return the first choice's {@code message} node (not the whole envelope)
   */
  private JsonNode callOpenAi(String apiKey, String context, List<ChatMessageDto> messages) {
    try {
      ObjectNode body = objectMapper.createObjectNode();
      body.put("model", properties.getModel());
      // Force a tool call — we don't want free-form chat that skips our structured outcomes.
      body.put("tool_choice", "required");
      body.set("tools", buildTools());

      // Chat Completions expects: system + prior user/assistant turns in order.
      ArrayNode apiMessages = body.putArray("messages");
      apiMessages.addObject().put("role", "system").put("content", SYSTEM_PROMPT + "\n" + context);
      for (ChatMessageDto msg : messages) {
        apiMessages.addObject().put("role", msg.role()).put("content", msg.content().trim());
      }

      String baseUrl = properties.getBaseUrl().replaceAll("/$", "");
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(baseUrl + "/chat/completions"))
              .timeout(Duration.ofSeconds(45))
              .header("Authorization", "Bearer " + apiKey)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        // Don't leak provider error bodies to the client.
        throw new ApiException(HttpStatus.BAD_GATEWAY, "OpenAI request failed");
      }

      // Typical shape: { choices: [ { message: { role, content?, tool_calls? } } ] }
      JsonNode root = objectMapper.readTree(response.body());
      JsonNode choiceMessage = root.path("choices").path(0).path("message");
      if (choiceMessage.isMissingNode() || choiceMessage.isNull()) {
        throw new ApiException(HttpStatus.BAD_GATEWAY, "OpenAI returned an empty response");
      }
      return choiceMessage;
    } catch (ApiException ex) {
      throw ex;
    } catch (IOException | InterruptedException ex) {
      // Preserve interrupt status so higher-level cancellation still works.
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new ApiException(HttpStatus.BAD_GATEWAY, "OpenAI request failed");
    }
  }

  /**
   * JSON Schema-like tool definitions OpenAI understands.
   *
   * <p>Each tool is a "function" the model can call. Arguments come back as a JSON string we parse.
   * These are not real Java methods — just contracts we interpret in {@link #mapToolResponse}.
   */
  private ArrayNode buildTools() {
    ArrayNode tools = objectMapper.createArrayNode();

    // Tool 1: missing info → ask the user (UI shows the question in chat).
    ObjectNode ask = tools.addObject();
    ask.put("type", "function");
    ObjectNode askFn = ask.putObject("function");
    askFn.put("name", "ask_clarification");
    askFn.put(
        "description",
        "Ask the user one clarifying question when date, time, or other required details are missing.");
    ObjectNode askParams = askFn.putObject("parameters");
    askParams.put("type", "object");
    askParams.putObject("properties").putObject("question").put("type", "string");
    askParams.putArray("required").add("question");
    askParams.put("additionalProperties", false);

    // Tool 2: enough info → structured event draft (UI opens EventModal).
    ObjectNode propose = tools.addObject();
    propose.put("type", "function");
    ObjectNode proposeFn = propose.putObject("function");
    proposeFn.put("name", "propose_event");
    proposeFn.put(
        "description",
        "Propose a complete single-event draft when start and end can be determined.");
    ObjectNode props = proposeFn.putObject("parameters");
    props.put("type", "object");
    ObjectNode fields = props.putObject("properties");
    fields.putObject("title").put("type", "string");
    ObjectNode description = fields.putObject("description");
    description.put("type", "string");
    description.put("nullable", true);
    fields.putObject("startAt").put("type", "string").put("description", "ISO-8601 Instant with Z");
    fields.putObject("endAt").put("type", "string").put("description", "ISO-8601 Instant with Z");
    fields.putObject("allDay").put("type", "boolean");
    ObjectNode category = fields.putObject("category");
    category.put("type", "string");
    category.putArray("enum").add("WORK").add("PERSONAL").add("HEALTH").add("OTHER");
    fields.putObject("recurrenceDetected").put("type", "boolean");
    props
        .putArray("required")
        .add("title")
        .add("startAt")
        .add("endAt")
        .add("allDay")
        .add("category")
        .add("recurrenceDetected");
    props.put("additionalProperties", false);

    return tools;
  }

  /**
   * Convert the model's message into our API response.
   *
   * <p>With {@code tool_choice=required} we expect {@code tool_calls[0]}. If the model somehow
   * returns plain text instead, we treat that text as a clarification question.
   */
  private AssistantResponse mapToolResponse(JsonNode message) {
    JsonNode toolCalls = message.path("tool_calls");
    if (toolCalls.isArray() && !toolCalls.isEmpty()) {
      // We only register two tools and ask for one call — take the first.
      JsonNode call = toolCalls.get(0);
      String name = call.path("function").path("name").asText("");
      // Arguments arrive as a JSON *string*, not a nested object.
      String argsRaw = call.path("function").path("arguments").asText("{}");
      JsonNode args;
      try {
        args = objectMapper.readTree(argsRaw);
      } catch (IOException ex) {
        throw new ApiException(HttpStatus.BAD_GATEWAY, "OpenAI returned invalid tool arguments");
      }

      if ("ask_clarification".equals(name)) {
        String question = textOrEmpty(args, "question").trim();
        if (question.isEmpty()) {
          question = "Could you share a bit more detail, like the date and time?";
        }
        return AssistantResponse.needInfo(question);
      }
      if ("propose_event".equals(name)) {
        EventDraftDto draft = normalizeDraft(args);
        String summary =
            "I've prepared a draft for “"
                + draft.title()
                + "”. Review it in the form and save when you're ready.";
        return AssistantResponse.ready(summary, draft);
      }
    }

    // Fallback: no usable tool call — still keep the conversation going.
    String content = message.path("content").asText("").trim();
    if (content.isEmpty()) {
      content = "Could you share the date and time for this event?";
    }
    return AssistantResponse.needInfo(content);
  }

  /**
   * Sanitize model output into something our event CRUD API would accept:
   * trim lengths, fix inverted times, map unknown categories to OTHER.
   */
  private EventDraftDto normalizeDraft(JsonNode node) {
    String title = textOrEmpty(node, "title").trim();
    if (title.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Could not extract an event title");
    }
    if (title.length() > 200) {
      title = title.substring(0, 200);
    }

    String description = textOrNull(node, "description");
    if (description != null) {
      description = description.trim();
      if (description.isEmpty()) {
        description = null;
      } else if (description.length() > 2000) {
        description = description.substring(0, 2000);
      }
    }

    Instant startAt = parseInstant(node, "startAt");
    Instant endAt = parseInstant(node, "endAt");
    if (!endAt.isAfter(startAt)) {
      // Guard against model mistakes; matches our "default 1 hour" rule.
      endAt = startAt.plus(Duration.ofHours(1));
    }

    boolean allDay = node.path("allDay").asBoolean(false);
    EventCategory category = parseCategory(node.path("category").asText(null));
    boolean recurrenceDetected = node.path("recurrenceDetected").asBoolean(false);
    String warning = recurrenceDetected ? RECURRENCE_WARNING : null;

    return new EventDraftDto(
        title, description, startAt, endAt, allDay, category, recurrenceDetected, warning);
  }

  /** Invalid / missing zone → UTC so parsing never crashes on bad client input. */
  private ZoneId resolveZone(String timeZone) {
    if (timeZone == null || timeZone.isBlank()) {
      return ZoneId.of("UTC");
    }
    try {
      return ZoneId.of(timeZone.trim());
    } catch (Exception ex) {
      return ZoneId.of("UTC");
    }
  }

  /** Event times are stored as UTC {@link Instant}s (ISO-8601 with {@code Z}). */
  private Instant parseInstant(JsonNode node, String field) {
    String raw = textOrEmpty(node, field);
    if (raw.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Could not extract event times");
    }
    try {
      return Instant.parse(raw);
    } catch (Exception ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Could not extract event times");
    }
  }

  private EventCategory parseCategory(String raw) {
    if (raw == null || raw.isBlank()) {
      return EventCategory.OTHER;
    }
    try {
      return EventCategory.valueOf(raw.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      return EventCategory.OTHER;
    }
  }

  private String textOrEmpty(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isMissingNode() || value.isNull() ? "" : value.asText("");
  }

  private String textOrNull(JsonNode node, String field) {
    JsonNode value = node.path(field);
    if (value.isMissingNode() || value.isNull()) {
      return null;
    }
    return value.asText(null);
  }
}
