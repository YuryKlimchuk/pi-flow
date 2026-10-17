package com.hydroyura.agents.piflow.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void resolve_whenAgentExists_shouldReturnDefinition() throws IOException {
        Path agents = tempDir.resolve("agents");
        Files.createDirectories(agents);
        Files.writeString(agents.resolve("analyst.md"), """
                ---
                name: analyst
                tools: read, grep
                ---
                Inspect code.
                """);

        AgentResolver resolver = new AgentResolver(List.of(agents));

        assertThat(resolver.resolve("analyst"))
                .isPresent()
                .hasValueSatisfying(def -> assertThat(def.name()).isEqualTo("analyst"));
    }

    @Test
    void resolve_whenMissing_shouldReturnEmpty() {
        AgentResolver resolver = new AgentResolver(List.of(tempDir));

        assertThat(resolver.resolve("nope")).isEmpty();
    }

    @Test
    void resolve_projectOverridesUser() throws IOException {
        Path project = tempDir.resolve("project");
        Path user = tempDir.resolve("user");
        Files.createDirectories(project);
        Files.createDirectories(user);
        Files.writeString(project.resolve("worker.md"), "---\nname: worker\ntools: read\n---\nProject worker.\n");
        Files.writeString(user.resolve("worker.md"), "---\nname: worker\ntools: read, bash\n---\nUser worker.\n");

        AgentResolver resolver = new AgentResolver(List.of(project, user));

        assertThat(resolver.resolve("worker"))
                .isPresent()
                .hasValueSatisfying(def -> assertThat(def.systemPrompt()).isEqualTo("Project worker."));
    }
}
