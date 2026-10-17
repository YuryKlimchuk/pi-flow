package com.hydroyura.agents.piflow.agent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/** Parses a Pi agent markdown file: YAML frontmatter plus a system-prompt body. */
public final class AgentFileParser {

    private AgentFileParser() {
    }

    public static AgentDefinition parse(String fileName, String content) {
        String yaml = "";
        String body = content;
        if (content != null && content.startsWith("---")) {
            int close = content.indexOf("\n---", 3);
            if (close >= 0) {
                yaml = content.substring(3, close).trim();
                body = content.substring(close + 4).trim();
            }
        }

        Map<String, Object> fm = Map.of();
        if (!yaml.isBlank()) {
            Object loaded = new Yaml().load(yaml);
            if (loaded instanceof Map<?, ?> map) {
                fm = castMap(map);
            }
        }

        String name = stringValue(fm.get("name"));
        if (name == null || name.isBlank()) {
            name = fileName;
        }
        String description = stringValue(fm.get("description"));
        String model = stringValue(fm.get("model"));
        List<String> tools = toolsValue(fm.get("tools"));

        return new AgentDefinition(name, description, model, tools, body == null ? "" : body);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
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
