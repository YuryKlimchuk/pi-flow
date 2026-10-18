package com.hydroyura.agents.piflow.agent.infrastructure;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * Parses a {@code system.md} agent resource: YAML frontmatter ({@code tools}) plus a system-prompt body.
 */
public final class SystemPromptParser {

    private SystemPromptParser() {
    }

    /** Parsed system-prompt resource: the tool allowlist and the prompt body. */
    public record SystemPrompt(List<String> tools, String body) {
    }

    public static SystemPrompt parse(String content) {
        String yaml = "";
        String body = content;
        if (content != null && content.startsWith("---")) {
            int close = content.indexOf("\n---", 3);
            if (close >= 0) {
                yaml = content.substring(3, close).trim();
                body = content.substring(close + 4).trim();
            }
        }

        List<String> tools = List.of();
        if (!yaml.isBlank()) {
            Object loaded = new Yaml().load(yaml);
            if (loaded instanceof Map<?, ?> map) {
                tools = toolsValue(map.get("tools"));
            }
        }
        return new SystemPrompt(tools, body == null ? "" : body);
    }

    private static List<String> toolsValue(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof String s) {
            return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(t -> !t.isEmpty())
                    .toList();
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(t -> !t.isEmpty())
                    .toList();
        }
        return List.of();
    }
}
