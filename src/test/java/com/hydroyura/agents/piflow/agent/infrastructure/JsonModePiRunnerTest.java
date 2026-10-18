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
import java.util.List;
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

    @Test
    void run_withEmptyStdout_shouldReturnFailureWithEmptyStreamDiagnostic() throws IOException {
        Path emptyPi = writeScript("fake-pi-empty", "#!/bin/sh\nexit 0\n");

        PiRunResult result = runner(emptyPi).run("hello");

        assertThat(result.status()).isEqualTo(PiRunStatus.FAILURE);
        assertThat(result.diagnostics()).contains("empty stream");
    }

    @Test
    void run_withMalformedOnlyStdout_shouldReturnFailureWithMalformedDiagnostic() throws IOException {
        Path malformedPi = writeScript("fake-pi-malformed", """
                #!/bin/sh
                printf '%s\\n' 'this is not json'
                printf '%s\\n' '{also not json'
                """);

        PiRunResult result = runner(malformedPi).run("hello");

        assertThat(result.status()).isEqualTo(PiRunStatus.FAILURE);
        assertThat(result.diagnostics()).contains("malformed stream");
    }

    @Test
    void run_withTextDeltasButNoCompletion_shouldReturnFailureWithMissingCompletionDiagnostic() throws IOException {
        Path noSettledPi = writeScript("fake-pi-no-settled", """
                #!/bin/sh
                printf '%s\\n' '{"type":"message_update","assistantMessageEvent":{"type":"text_delta","contentIndex":0,"delta":"hello"}}'
                printf '%s\\n' '{"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"hello"}],"stopReason":"end"}}'
                """);

        PiRunResult result = runner(noSettledPi).run("hello");

        assertThat(result.status()).isEqualTo(PiRunStatus.FAILURE);
        assertThat(result.diagnostics()).contains("missing completion");
        assertThat(result.finalText()).isEqualTo("hello");
    }

    @Test
    void run_withUnrecognizedProtocol_shouldReturnFailureWithUnsupportedProtocolDiagnostic() throws IOException {
        Path unknownPi = writeScript("fake-pi-unknown", """
                #!/bin/sh
                printf '%s\\n' '{"type":"session","version":3,"id":"fake"}'
                printf '%s\\n' '{"type":"agent_start"}'
                """);

        PiRunResult result = runner(unknownPi).run("hello");

        assertThat(result.status()).isEqualTo(PiRunStatus.FAILURE);
        assertThat(result.diagnostics()).contains("unsupported protocol");
    }

    @Test
    void run_withAbortedSettlement_shouldReturnFailureWithAbortDiagnostic() throws IOException {
        Path abortedPi = writeScript("fake-pi-aborted", """
                #!/bin/sh
                printf '%s\\n' '{"type":"agent_settled","aborted":true}'
                """);

        PiRunResult result = runner(abortedPi).run("hello");

        assertThat(result.status()).isEqualTo(PiRunStatus.FAILURE);
        assertThat(result.diagnostics()).contains("aborted");
    }

    @Test
    void run_withErrorThenRecoveredSettlement_shouldReturnSuccess() throws IOException {
        Path recoveredPi = writeScript("fake-pi-recovered", """
                #!/bin/sh
                printf '%s\\n' '{"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"oops"}],"stopReason":"error"}}'
                printf '%s\\n' '{"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"fixed"}],"stopReason":"end"}}'
                printf '%s\\n' '{"type":"agent_settled","aborted":false}'
                """);

        PiRunResult result = runner(recoveredPi).run("hello");

        assertThat(result.status()).isEqualTo(PiRunStatus.SUCCESS);
        assertThat(result.finalText()).isEqualTo("fixed");
    }

    @Test
    void run_withNonexistentCommand_shouldReturnProcessStartFailure() {
        JsonModePiRunner runner = new JsonModePiRunner(
                new PiProperties(tempDir.resolve("does-not-exist-pi").toString(), Duration.ofSeconds(10), null),
                new ResourceAgentResolver(
                        new AgentProperties(Map.of(), Map.of()),
                        new PathMatchingResourcePatternResolver()));

        PiRunResult result = runner.run("hello");

        assertThat(result.status()).isEqualTo(PiRunStatus.FAILURE);
        assertThat(result.diagnostics()).contains("Failed to start Pi");
    }

    @Test
    void diagnostics_shouldDifferBetweenFailureCategories() throws IOException {
        JsonModePiRunner timeoutRunner = new JsonModePiRunner(
                new PiProperties(fakePi.toString(), Duration.ofSeconds(1), null),
                new ResourceAgentResolver(
                        new AgentProperties(Map.of(), Map.of()),
                        new PathMatchingResourcePatternResolver()));
        PiRunResult timeout = timeoutRunner.run("hang");

        Path emptyPi = writeScript("fake-pi-empty-distinct", "#!/bin/sh\nexit 0\n");
        PiRunResult protocol = runner(emptyPi).run("hello");

        JsonModePiRunner startRunner = new JsonModePiRunner(
                new PiProperties(tempDir.resolve("no-such-pi-distinct").toString(), Duration.ofSeconds(10), null),
                new ResourceAgentResolver(
                        new AgentProperties(Map.of(), Map.of()),
                        new PathMatchingResourcePatternResolver()));
        PiRunResult start = startRunner.run("hello");

        assertThat(timeout.status()).isEqualTo(PiRunStatus.TIMEOUT);
        assertThat(timeout.diagnostics())
                .contains("timed out")
                .doesNotContain("Failed to start Pi")
                .doesNotContain("empty stream");

        assertThat(protocol.diagnostics())
                .contains("empty stream")
                .doesNotContain("timed out")
                .doesNotContain("Failed to start Pi");

        assertThat(start.diagnostics())
                .contains("Failed to start Pi")
                .doesNotContain("timed out")
                .doesNotContain("empty stream");
    }

    @Test
    void run_withHangingPrompt_shouldJoinReaderThreadsBeforeTimeout() {
        JsonModePiRunner runner = new JsonModePiRunner(
                new PiProperties(fakePi.toString(), Duration.ofSeconds(1), null),
                new ResourceAgentResolver(
                        new AgentProperties(Map.of(), Map.of()),
                        new PathMatchingResourcePatternResolver()));

        PiRunResult result = runner.run("hang");

        assertThat(result.status()).isEqualTo(PiRunStatus.TIMEOUT);
        boolean strayReaderAlive = Thread.getAllStackTraces().keySet().stream()
                .filter(Thread::isAlive)
                .map(Thread::getName)
                .anyMatch(name -> name.equals("pi-stdout") || name.equals("pi-stderr"));
        assertThat(strayReaderAlive).isFalse();
    }

    @Test
    void run_withFailingAgent_shouldDeleteTempPromptAndSkillFiles() throws IOException {
        Path argsFile = tempDir.resolve("agent-args.txt");
        Path failingPi = writeScript("fake-pi-failing-agent", """
                #!/bin/sh
                printf '%s\\n' "$@" > ARGSFILE
                printf '%s\\n' '{"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"oops"}],"stopReason":"error"}}'
                printf '%s\\n' '{"type":"agent_settled","aborted":false}'
                """.replace("ARGSFILE", argsFile.toString()));

        JsonModePiRunner runner = new JsonModePiRunner(
                new PiProperties(failingPi.toString(), Duration.ofSeconds(10), null),
                resolver("developer", "cheap", "deepseek/deepseek-v4-pro"));

        PiRunResult result = runner.run("developer", "hello", null);

        assertThat(result.status()).isEqualTo(PiRunStatus.FAILURE);
        List<String> args = Files.readAllLines(argsFile);
        assertTempFileRemoved(args, "--append-system-prompt");
        assertTempFileRemoved(args, "--skill");
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

    private static Path writeScript(String name, String body) throws IOException {
        Path script = tempDir.resolve(name);
        Files.writeString(script, body);
        if (!script.toFile().setExecutable(true)) {
            throw new IllegalStateException("could not make " + name + " executable");
        }
        return script;
    }

    private static void assertTempFileRemoved(List<String> args, String flag) {
        int idx = args.indexOf(flag);
        assertThat(idx).isNotNegative();
        assertThat(args.size()).isGreaterThan(idx + 1);
        assertThat(Files.notExists(Path.of(args.get(idx + 1)))).isTrue();
    }
}
