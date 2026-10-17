package com.hydroyura.agents.piflow.agent;

import java.util.List;

/**
 * A Pi agent definition resolved from a markdown file (YAML frontmatter + system prompt).
 *
 * @param name         agent name
 * @param description  short description
 * @param model        model id, or {@code null} to inherit the default
 * @param tools        tool allowlist (empty means "no explicit allowlist")
 * @param systemPrompt role instructions appended to Pi's system prompt
 */
public record AgentDefinition(String name, String description, String model, List<String> tools, String systemPrompt) {

    public AgentDefinition {
        tools = tools != null ? List.copyOf(tools) : List.of();
    }
}
