package com.hydroyura.agents.piflow.agent.domain;

import java.util.List;

/**
 * A resolved agent: name, resolved model id, tool allowlist, system prompt, and skills.
 *
 * @param name         role name
 * @param model        resolved model id
 * @param tools        tool allowlist
 * @param systemPrompt role system prompt
 * @param skills       the agent's skills (name + markdown content)
 */
public record AgentDefinition(String name, String model, List<String> tools, String systemPrompt, List<Skill> skills) {

    public AgentDefinition {
        tools = tools != null ? List.copyOf(tools) : List.of();
        skills = skills != null ? List.copyOf(skills) : List.of();
    }
}
