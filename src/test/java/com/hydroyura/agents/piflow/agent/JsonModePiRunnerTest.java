package com.hydroyura.agents.piflow.agent;

import static org.assertj.core.api.Assertions.assertThat;

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
                new AgentResolver(List.of()));
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
                new AgentResolver(List.of()));

        PiRunResult result = runner.run("hang");

        assertThat(result.status()).isEqualTo(PiRunStatus.TIMEOUT);
    }

    @Test
    void run_withAgent_shouldPassAgentFlags() throws IOException {
        Path agentDir = tempDir.resolve("agents");
        Files.createDirectories(agentDir);
        Files.writeString(agentDir.resolve("analyst.md"), """
                ---
                name: analyst
                tools: read, grep
                model: deepseek/deepseek-v4-pro
                ---
                You are an analyst.
                """);

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
                new AgentResolver(List.of(agentDir)));

        PiRunResult result = runner.run("analyst", "hello");

        assertThat(result.status()).isEqualTo(PiRunStatus.SUCCESS);
        assertThat(result.finalText()).isEqualTo("ok");
        assertThat(result.diagnostics())
                .contains("--model", "deepseek/deepseek-v4-pro")
                .contains("--tools", "read,grep")
                .contains("--append-system-prompt")
                .contains("--no-session");
    }

    @Test
    void run_withUnknownAgent_shouldReturnFailure() {
        JsonModePiRunner runner = runner(fakePi);

        PiRunResult result = runner.run("unknown-agent", "hello");

        assertThat(result.status()).isEqualTo(PiRunStatus.FAILURE);
        assertThat(result.diagnostics()).contains("Unknown agent: unknown-agent");
    }
}
