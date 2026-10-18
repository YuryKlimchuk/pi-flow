package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.agents.piflow.workflow.application.ValidationResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenSpecChangeValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void validChange_shouldReportSuccess() throws IOException {
        Path repo = fixtureRepo("valid-change");

        ValidationResult result = validator(exitOpenspec(0, "")).validate(repo.toString(), "valid-change");

        assertThat(result.passed()).isTrue();
        assertThat(result.reason()).isEmpty();
    }

    @Test
    void invalidChange_shouldReportFailureWithCapturedOutput() throws IOException {
        Path repo = fixtureRepo("invalid-change");

        ValidationResult result = validator(exitOpenspec(1, "spec is missing a required scenario"))
                .validate(repo.toString(), "invalid-change");

        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("missing a required scenario");
    }

    @Test
    void processThatFailsToStart_shouldReportFailureDistinguishably() throws IOException {
        Path repo = fixtureRepo("any-change");
        OpenSpecChangeValidator validator = new OpenSpecChangeValidator(
                tempDir.resolve("no-such-openspec").toString(), Duration.ofSeconds(5));

        ValidationResult result = validator.validate(repo.toString(), "any-change");

        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("failed to start");
    }

    private Path fixtureRepo(String changeId) throws IOException {
        Path repo = Files.createDirectories(tempDir.resolve("repo-" + changeId));
        Path changeDir = Files.createDirectories(repo.resolve("openspec/changes/" + changeId));
        Files.writeString(changeDir.resolve("proposal.md"), "# Proposal\n\nWhy: test\n");
        return repo;
    }

    private OpenSpecChangeValidator validator(Path openspec) {
        return new OpenSpecChangeValidator(openspec.toString(), Duration.ofSeconds(10));
    }

    private Path exitOpenspec(int exitCode, String output) throws IOException {
        Path data = Files.createTempFile(tempDir, "openspec-validate-out", ".txt");
        Files.writeString(data, output);
        Path script = Files.createTempFile(tempDir, "fake-openspec", ".sh");
        Files.writeString(script, "#!/bin/sh\ncat '" + data + "'\nexit " + exitCode + "\n");
        if (!script.toFile().setExecutable(true)) {
            throw new IllegalStateException("could not make fake openspec executable");
        }
        return script;
    }
}
