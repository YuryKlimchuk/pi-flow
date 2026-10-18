package com.hydroyura.agents.piflow.agent.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.agents.piflow.agent.api.PiRunResult;
import com.hydroyura.agents.piflow.agent.api.PiRunStatus;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class JsonModePiRunnerTest {

    @TempDir
    static Path tempDir;

    private static Path fakePi;

    @BeforeAll
    static void setUpFakePi() throws IOException {
        String script;
        try (InputStream in = JsonModePiRunnerTest.class.getResourceAsStream("/fake-pi")) {
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

    private static JsonModePiRunner runner(Path command) {
        return new JsonModePiRunner(
                new PiProperties(command.toString(), Duration.ofSeconds(10), null),
                new ResourceAgentResolver(
                        new AgentProperties(Map.of(), Map.of()),
                        new PathMatchingResourcePatternResolver()));
    }

    private static ResourceAgentResolver resolver(String role, String tier, String model) {
        return new ResourceAgentResolver(
                new AgentProperties(Map.of(tier, model), Map.of(role, tier)),
                new PathMatchingResourcePatternResolver());
    }

    @Test
    void fakePiFixture_printsExpectedSuccessJsonl() throws Exception {
        Process process = new ProcessBuilder(fakePi.toString(), "--mode", "json", "say hello").start();
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.waitFor();

        assertThat(out)
                .contains("\"type\":\"agent_settled\"")
                .contains("\"delta\":\"Hello \"")
                .contains("Hello world!");
    }

    @Test
    void run_withSuccessfulPrompt_shouldReturnSuccessAndAuthoritativeText() {
        PiRunResult result = runner(fakePi).run("say hello");

        assertThat(result.status()).isEqualTo(PiRunStatus.SUCCESS);
        assertThat(result.finalText()).isEqualTo("Hello world!");
    }

    @Test
    void run_withFailingPrompt_shouldReturnFailure() {
        PiRunResult result = runner(fakePi).run("fail now");

        assertThat(result.status()).isEqualTo(PiRunStatus.FAILURE);
        assertThat(result.diagnostics()).isEqualTo("error");
    }

    @Test
    void run_withHangingPrompt_shouldReturnTimeout() {
        JsonModePiRunner runner = new JsonModePiRunner(
                new PiProperties(fakePi.toString(), Duration.ofSeconds(1), null),
                new ResourceAgentResolver(
                        new AgentProperties(Map.of(), Map.of()),
                        new PathMatchingResourcePatternResolver()));

        PiRunResult result = runner.run("hang");

        assertThat(result.status()).isEqualTo(PiRunStatus.TIMEOUT);
    }

    @Test
    void run_withAgent_shouldPassAgentFlags() throws IOException {
        Path agentPi = tempDir.resolve("fake-pi-agent");
        Files.writeString(agentPi, """
                #!/bin/sh
                echo "ARGS: $*" >&2
                for last_arg; do :; done
                prompt="$last_arg"
                case "$prompt" in
                  *fail*)
                    echo '{"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"oops"}],"stopReason":"error"}}'
                    echo '{"type":"agent_settled","aborted":false}'
                    ;;
                  *)
                    echo '{"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"ok"}]}}'
                    echo '{"type":"agent_settled","aborted":false}'
                    ;;
                esac
                """);
        if (!agentPi.toFile().setExecutable(true)) {
            throw new IllegalStateException("could not make fake-pi-agent executable");
        }

        JsonModePiRunner runner = new JsonModePiRunner(
                new PiProperties(agentPi.toString(), Duration.ofSeconds(10), null),
                resolver("analyst", "cheap", "deepseek/deepseek-v4-pro"));

        PiRunResult result = runner.run("analyst", "hello", null);

        assertThat(result.status()).isEqualTo(PiRunStatus.SUCCESS);
        assertThat(result.finalText()).isEqualTo("ok");
        assertThat(result.diagnostics())
                .contains("--model", "deepseek/deepseek-v4-pro")
                .contains("--tools", "read,grep,find,ls")
                .contains("--append-system-prompt")
                .contains("--no-session");
    }

    @Test
    void run_withAgentWithSkill_shouldPassSkill() throws IOException {
        Path agentPi = tempDir.resolve("fake-pi-skill");
        Files.writeString(agentPi, """
                #!/bin/sh
                echo "ARGS: $*" >&2
                echo '{"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"ok"}]}}'
                echo '{"type":"agent_settled","aborted":false}'
                """);
        if (!agentPi.toFile().setExecutable(true)) {
            throw new IllegalStateException("could not make fake-pi-skill executable");
        }

        JsonModePiRunner runner = new JsonModePiRunner(
                new PiProperties(agentPi.toString(), Duration.ofSeconds(10), null),
                resolver("developer", "cheap", "deepseek/deepseek-v4-pro"));

        PiRunResult result = runner.run("developer", "hello", null);

        assertThat(result.status()).isEqualTo(PiRunStatus.SUCCESS);
        assertThat(result.diagnostics())
                .contains("--model", "deepseek/deepseek-v4-pro")
                .contains("--skill")
                .contains("--append-system-prompt");
    }

    @Test
    void run_withUnknownAgent_shouldReturnFailure() {
        JsonModePiRunner runner = runner(fakePi);

        PiRunResult result = runner.run("unknown-agent", "hello", null);

        assertThat(result.status()).isEqualTo(PiRunStatus.FAILURE);
        assertThat(result.diagnostics()).contains("unknown-agent");
    }

    @Test
    void run_withWorkingDirectory_shouldLaunchPiInProvidedDirectory() throws IOException {
        Path targetDir = tempDir.resolve("target-repo");
        Files.createDirectories(targetDir);

        JsonModePiRunner runner = new JsonModePiRunner(
                new PiProperties(cwdEchoingPi().toString(), Duration.ofSeconds(10), null),
                resolver("analyst", "cheap", "deepseek/deepseek-v4-pro"));

        PiRunResult result = runner.run("analyst", "hello", targetDir.toString());

        assertThat(result.status()).isEqualTo(PiRunStatus.SUCCESS);
        assertThat(result.diagnostics()).contains("CWD=" + targetDir);
    }

    @Test
    void run_withWorkingDirectory_shouldPreferExplicitOverGlobalDefault() throws IOException {
        Path globalDir = tempDir.resolve("global");
        Files.createDirectories(globalDir);
        Path explicitDir = tempDir.resolve("explicit");
        Files.createDirectories(explicitDir);

        JsonModePiRunner runner = new JsonModePiRunner(
                new PiProperties(cwdEchoingPi().toString(), Duration.ofSeconds(10), globalDir.toString()),
                resolver("analyst", "cheap", "deepseek/deepseek-v4-pro"));

        PiRunResult result = runner.run("analyst", "hello", explicitDir.toString());

        assertThat(result.status()).isEqualTo(PiRunStatus.SUCCESS);
        assertThat(result.diagnostics()).contains("CWD=" + explicitDir);
        assertThat(result.diagnostics()).doesNotContain("CWD=" + globalDir);
    }

    private static Path cwdEchoingPi() throws IOException {
        Path cwdPi = tempDir.resolve("fake-pi-cwd");
        Files.writeString(cwdPi, """
                #!/bin/sh
                echo "CWD=$PWD" >&2
                printf '%s\\n' '{"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"ok"}]}}'
                printf '%s\\n' '{"type":"agent_settled","aborted":false}'
                """);
        if (!cwdPi.toFile().setExecutable(true)) {
            throw new IllegalStateException("could not make fake-pi-cwd executable");
        }
        return cwdPi;
    }
}
