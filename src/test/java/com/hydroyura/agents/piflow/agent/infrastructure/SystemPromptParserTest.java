package com.hydroyura.agents.piflow.agent.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SystemPromptParserTest {

    @Test
    void parse_shouldExtractToolsAndBody() {
        String content = """
                ---
                tools: read, grep, find
                ---
                You are an analyst. Inspect the project.
                """;

        SystemPromptParser.SystemPrompt prompt = SystemPromptParser.parse(content);

        assertThat(prompt.tools()).containsExactly("read", "grep", "find");
        assertThat(prompt.body()).contains("You are an analyst");
    }

    @Test
    void parse_withoutFrontmatter_shouldTreatWholeFileAsBody() {
        SystemPromptParser.SystemPrompt prompt = SystemPromptParser.parse("Just a prompt body");

        assertThat(prompt.tools()).isEmpty();
        assertThat(prompt.body()).isEqualTo("Just a prompt body");
    }

    @Test
    void parse_withListTools_shouldParseList() {
        String content = """
                ---
                tools:
                  - read
                  - grep
                ---
                Review carefully.
                """;

        SystemPromptParser.SystemPrompt prompt = SystemPromptParser.parse(content);

        assertThat(prompt.tools()).containsExactly("read", "grep");
    }
}
