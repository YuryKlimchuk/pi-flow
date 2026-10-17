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

    public void accept(String line) {
        JsonNode node;
        try {
            node = MAPPER.readTree(line);
        } catch (JacksonException e) {
            log.debug("Skipping malformed JSONL record: {}", e.getMessage());
            return; // skip malformed JSONL record
        }

        switch (node.path("type").asText()) {
            case "message_update" -> handleMessageUpdate(node.path("assistantMessageEvent"));
            case "message_end" -> handleMessageEnd(node.path("message"));
            case "agent_settled" -> handleSettled(node);
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
            if ("error".equals(stopReason) || "aborted".equals(stopReason)) {
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
}
