package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.agents.piflow.agent.api.PiRunner;
import com.hydroyura.agents.piflow.agent.infrastructure.AgentProperties;
import com.hydroyura.agents.piflow.agent.infrastructure.JsonModePiRunner;
import com.hydroyura.agents.piflow.agent.infrastructure.PiProperties;
import com.hydroyura.agents.piflow.agent.infrastructure.ResourceAgentResolver;
import com.hydroyura.agents.piflow.workflow.api.StageOutcome;
import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.application.StageRunner;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class AgentStageRunnerTest {

    @TempDir
    static Path tempDir;

    @Test
    void run_shouldInvokePiUnderRoleInTargetRepository() throws IOException {
        Path targetRepo = tempDir.resolve("target-repo");
        Files.createDirectories(targetRepo);

        PiRunner piRunner = new JsonModePiRunner(
                new PiProperties(cwdPi().toString(), Duration.ofSeconds(10), null),
                new ResourceAgentResolver(
                        new AgentProperties(Map.of("cheap", "fake-model"), Map.of("analyst", "cheap")),
                        new PathMatchingResourcePatternResolver()));
        StageRunner runner = new AgentStageRunner(piRunner);

        Workflow workflow = new Workflow(WorkflowId.generate(), "task", targetRepo.toString());
        StageOutcome outcome = runner.run(WorkflowStage.PROJECT_ANALYSIS, workflow);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.output()).isEqualTo("ok");
        assertThat(Files.readString(targetRepo.resolve("cwd.txt")).trim())
                .isEqualTo(targetRepo.toString());
    }

    private static Path cwdPi() throws IOException {
        Path cwdPi = tempDir.resolve("fake-pi-cwd");
        Files.writeString(cwdPi, """
                #!/bin/sh
                printf '%s\\n' "$PWD" > cwd.txt
                printf '%s\\n' '{"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"ok"}]}}'
                printf '%s\\n' '{"type":"agent_settled","aborted":false}'
                """);
        if (!cwdPi.toFile().setExecutable(true)) {
            throw new IllegalStateException("could not make fake-pi-cwd executable");
        }
        return cwdPi;
    }
}
