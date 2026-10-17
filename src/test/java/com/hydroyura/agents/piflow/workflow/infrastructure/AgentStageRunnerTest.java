package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.agents.piflow.agent.api.PiRunner;
import com.hydroyura.agents.piflow.agent.infrastructure.FileAgentResolver;
import com.hydroyura.agents.piflow.agent.infrastructure.JsonModePiRunner;
import com.hydroyura.agents.piflow.agent.infrastructure.PiProperties;
import com.hydroyura.agents.piflow.workflow.api.StageOutcome;
import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.application.StageRunner;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentStageRunnerTest {

    @TempDir
    static Path tempDir;

    private static Path fakePi;

    @BeforeAll
    static void setUp() throws IOException {
        String script;
        try (InputStream in = AgentStageRunnerTest.class.getResourceAsStream("/fake-pi")) {
            if (in == null) {
                throw new IllegalStateException("fake-pi fixture missing from test resources");
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        fakePi = tempDir.resolve("fake-pi");
        Files.writeString(fakePi, script);
        if (!fakePi.toFile().setExecutable(true)) {
            throw new IllegalStateException("could not make fake-pi executable");
        }
    }

    @Test
    void run_shouldInvokePiUnderRole() throws IOException {
        Path agentDir = tempDir.resolve("agents");
        Files.createDirectories(agentDir);
        Files.writeString(agentDir.resolve("analyst.md"), "---\nname: analyst\n---\nYou are an analyst.\n");

        PiRunner piRunner = new JsonModePiRunner(
                new PiProperties(fakePi.toString(), Duration.ofSeconds(10), null),
                new FileAgentResolver(List.of(agentDir)));
        StageRunner runner = new AgentStageRunner(piRunner);

        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");
        StageOutcome outcome = runner.run(WorkflowStage.PROJECT_ANALYSIS, workflow);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.output()).isEqualTo("Hello world!");
    }
}
