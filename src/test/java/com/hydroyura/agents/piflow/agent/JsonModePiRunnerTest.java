package com.hydroyura.agents.piflow.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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
        PiRunner runner = new JsonModePiRunner(new PiProperties(fakePi.toString(), Duration.ofSeconds(10), null));

        PiRunResult result = runner.run("say hello");

        assertThat(result.status()).isEqualTo(PiRunStatus.SUCCESS);
        assertThat(result.finalText()).isEqualTo("Hello world!");
    }

    @Test
    void run_withFailingPrompt_shouldReturnFailure() {
        PiRunner runner = new JsonModePiRunner(new PiProperties(fakePi.toString(), Duration.ofSeconds(10), null));

        PiRunResult result = runner.run("fail now");

        assertThat(result.status()).isEqualTo(PiRunStatus.FAILURE);
        assertThat(result.diagnostics()).isEqualTo("error");
    }

    @Test
    void run_withHangingPrompt_shouldReturnTimeout() {
        PiRunner runner = new JsonModePiRunner(new PiProperties(fakePi.toString(), Duration.ofSeconds(1), null));

        PiRunResult result = runner.run("hang");

        assertThat(result.status()).isEqualTo(PiRunStatus.TIMEOUT);
    }
}
