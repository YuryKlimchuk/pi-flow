package com.hydroyura.agents.piflow.agent.domain;

/**
 * A standalone skill: its name and markdown content.
 *
 * @param name    skill name (used to name the temp file passed to {@code --skill})
 * @param content skill instructions (markdown)
 */
public record Skill(String name, String content) {
}
