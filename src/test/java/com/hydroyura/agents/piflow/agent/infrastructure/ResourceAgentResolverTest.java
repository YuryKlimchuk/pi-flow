package com.hydroyura.agents.piflow.agent.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.agents.piflow.agent.application.AgentResolution;
import com.hydroyura.agents.piflow.agent.domain.AgentDefinition;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class ResourceAgentResolverTest {

    private final PathMatchingResourcePatternResolver resources = new PathMatchingResourcePatternResolver();

    private static ResourceAgentResolver resolver(Map<String, String> tiers, Map<String, String> roles) {
        return new ResourceAgentResolver(new AgentProperties(tiers, roles), new PathMatchingResourcePatternResolver());
    }

    @Test
    void resolve_whenAgentExists_shouldResolveDefinition() {
        ResourceAgentResolver resolver = resolver(
                Map.of("cheap", "deepseek/deepseek-v4-pro"),
                Map.of("analyst", "cheap"));

        AgentResolution result = resolver.resolve("analyst");

        assertThat(result.isResolved()).isTrue();
        AgentDefinition def = result.definition();
        assertThat(def.model()).isEqualTo("deepseek/deepseek-v4-pro");
        assertThat(def.tools()).contains("read");
        assertThat(def.systemPrompt()).isNotBlank();
    }

    @Test
    void resolve_whenRoleNotConfigured_shouldReturnError() {
        ResourceAgentResolver resolver = resolver(Map.of("cheap", "x"), Map.of());

        AgentResolution result = resolver.resolve("analyst");

        assertThat(result.isResolved()).isFalse();
        assertThat(result.error()).contains("analyst");
    }

    @Test
    void resolve_whenTierNotMapped_shouldReturnError() {
        ResourceAgentResolver resolver = resolver(Map.of(), Map.of("analyst", "cheap"));

        AgentResolution result = resolver.resolve("analyst");

        assertThat(result.isResolved()).isFalse();
        assertThat(result.error()).contains("cheap");
    }

    @Test
    void resolve_shouldDiscoverSkills() {
        ResourceAgentResolver resolver = resolver(
                Map.of("cheap", "deepseek/deepseek-v4-pro"),
                Map.of("developer", "cheap"));

        AgentResolution result = resolver.resolve("developer");

        assertThat(result.isResolved()).isTrue();
        assertThat(result.definition().skills()).isNotEmpty();
        assertThat(result.definition().skills().get(0).name()).isNotBlank();
        assertThat(result.definition().skills().get(0).content()).isNotBlank();
    }
}
