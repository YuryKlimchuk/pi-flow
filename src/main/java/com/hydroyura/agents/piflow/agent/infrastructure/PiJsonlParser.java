package com.hydroyura.agents.piflow.agent.infrastructure;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Assembles Pi's JSONL event stream into a structured outcome.
 *
 * <p>Pure (no Spring), so it can be unit-tested against fixture records.</p>
 */
@Slf4j
public final class PiJsonlParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final StringBuilder streamedText = new StringBuilder();
    private String authoritativeText;
    private boolean settled;
    private boolean failed;
    private String failureReason;
    private boolean sawAnyLine;
    private boolean sawParsedLine;
    private boolean sawRecognizedEvent;
    private boolean lastAssistantMessageEndError;

    public void accept(String line) {
        sawAnyLine = true;
        JsonNode node;
        try {
            node = MAPPER.readTree(line);
        } catch (JacksonException e) {
            log.debug("Skipping malformed JSONL record: {}", e.getMessage());
            return; // skip malformed JSONL record
        }
        sawParsedLine = true;

        switch (node.path("type").asText()) {
            case "message_update" -> {
                sawRecognizedEvent = true;
                handleMessageUpdate(node.path("assistantMessageEvent"));
            }
            case "message_end" -> {
                sawRecognizedEvent = true;
                handleMessageEnd(node.path("message"));
            }
            case "agent_settled" -> {
                sawRecognizedEvent = true;
                handleSettled(node);
            }
            default -> {
            }
        }
    }

    private void handleMessageUpdate(JsonNode event) {
        if ("text_delta".equals(event.path("type").asText())) {
            streamedText.append(event.path("delta").asText());
        }
    }

    private void handleMessageEnd(JsonNode message) {
        if ("assistant".equals(message.path("role").asText())) {
            authoritativeText = extractText(message.path("content"));
            String stopReason = message.path("stopReason").asText();
            boolean isError = "error".equals(stopReason) || "aborted".equals(stopReason);
            lastAssistantMessageEndError = isError;
            if (isError) {
                failed = true;
                failureReason = stopReason;
            }
        }
    }

    private void handleSettled(JsonNode node) {
        settled = true;
        if (node.path("aborted").asBoolean(false)) {
            failed = true;
            failureReason = "aborted";
        } else if (!lastAssistantMessageEndError) {
            // A later successful message_end followed by a non-aborted settlement
            // demonstrates recovery from an earlier intermediate error.
            failed = false;
            failureReason = null;
        }
    }

    private String extractText(JsonNode content) {
        if (content.isTextual()) {
            return content.asText();
        }
        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    sb.append(block.path("text").asText());
                }
            }
            return sb.toString();
        }
        return "";
    }

    public String finalText() {
        return authoritativeText != null ? authoritativeText : streamedText.toString();
    }

    public boolean failed() {
        return failed;
    }

    public boolean settled() {
        return settled;
    }

    public String failureReason() {
        return failureReason;
    }

    /** True once at least one line matched a supported Pi JSON-mode event type. */
    public boolean supportedProtocolObserved() {
        return sawRecognizedEvent;
    }

    /** True once an {@code agent_settled} event has been observed. */
    public boolean hasRecognizedCompletion() {
        return settled;
    }

    /** True once at least one stdout line has been fed to {@link #accept(String)}. */
    public boolean sawAnyLine() {
        return sawAnyLine;
    }

    /** True once at least one line parsed as valid JSON (regardless of its type). */
    public boolean sawParsedLine() {
        return sawParsedLine;
    }
}
