package com.hydroyura.agents.piflow.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AgentFileParserTest {

    @Test
    void parse_shouldExtractFrontmatterAndBody() {
        String content = """
                ---
                name: analyst
                description: Inspect code
                tools: read, grep, find
                model: deepseek/deepseek-v4-pro
                ---
                You are an analyst. Inspect the project.
                """;

        AgentDefinition def = AgentFileParser.parse("analyst", content);

        assertThat(def.name()).isEqualTo("analyst");
        assertThat(def.description()).isEqualTo("Inspect code");
        assertThat(def.model()).isEqualTo("deepseek/deepseek-v4-pro");
        assertThat(def.tools()).containsExactly("read", "grep", "find");
        assertThat(def.systemPrompt()).contains("You are an analyst");
    }

    @Test
    void parse_withoutFrontmatter_shouldTreatWholeFileAsPrompt() {
        AgentDefinition def = AgentFileParser.parse("fallback", "Just a prompt body");

        assertThat(def.name()).isEqualTo("fallback");
        assertThat(def.model()).isNull();
        assertThat(def.tools()).isEmpty();
        assertThat(def.systemPrompt()).isEqualTo("Just a prompt body");
    }

    @Test
    void parse_withListTools_shouldParseList() {
        String content = """
                ---
                name: reviewer
                tools:
                  - read
                  - grep
                ---
                Review carefully.
                """;

        AgentDefinition def = AgentFileParser.parse("reviewer", content);

        assertThat(def.tools()).containsExactly("read", "grep");
    }
}
