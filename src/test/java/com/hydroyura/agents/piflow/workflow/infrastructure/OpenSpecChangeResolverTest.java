package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.agents.piflow.workflow.application.ChangeResolution;
import com.hydroyura.agents.piflow.workflow.application.ChangeResolutionStatus;
import com.hydroyura.agents.piflow.workflow.application.ChangeResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenSpecChangeResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void matchingSingleActiveChange_shouldResolve() throws IOException {
        Path repo = Files.createDirectories(tempDir.resolve("repo"));
        ChangeResolver resolver = resolver(fakeOpenspec(
                "{\"changes\":[{\"name\":\"real-change\",\"status\":\"in-progress\"}],\"root\":{}}"));

        ChangeResolution resolution = resolver.resolve(repo.toString(), "real-change");

        assertThat(resolution.status()).isEqualTo(ChangeResolutionStatus.RESOLVED);
        assertThat(resolution.changeId()).contains("real-change");
        assertThat(resolution.reason()).isEmpty();
    }

    @Test
    void identifierMatchingNoActiveChange_shouldBeNotFound() throws IOException {
        Path repo = Files.createDirectories(tempDir.resolve("repo"));
        ChangeResolver resolver = resolver(fakeOpenspec(
                "{\"changes\":[{\"name\":\"other-change\",\"status\":\"in-progress\"}],\"root\":{}}"));

        ChangeResolution resolution = resolver.resolve(repo.toString(), "missing");

        assertThat(resolution.status()).isEqualTo(ChangeResolutionStatus.NOT_FOUND);
        assertThat(resolution.changeId()).isEmpty();
    }

    @Test
    void identifierMatchingMoreThanOneActiveChange_shouldBeAmbiguous() throws IOException {
        Path repo = Files.createDirectories(tempDir.resolve("repo"));
        ChangeResolver resolver = resolver(fakeOpenspec(
                "{\"changes\":[{\"name\":\"dup\",\"status\":\"in-progress\"},{\"name\":\"dup\",\"status\":\"in-progress\"}],\"root\":{}}"));

        ChangeResolution resolution = resolver.resolve(repo.toString(), "dup");

        assertThat(resolution.status()).isEqualTo(ChangeResolutionStatus.AMBIGUOUS);
        assertThat(resolution.changeId()).isEmpty();
    }

    @Test
    void pathEscapingIdentifier_shouldRejectWithoutInvokingCli() throws IOException {
        Path repo = Files.createDirectories(tempDir.resolve("repo"));
        ChangeResolver resolver = new OpenSpecChangeResolver(
                tempDir.resolve("no-such-openspec").toString(), Duration.ofSeconds(5));

        assertThat(resolver.resolve(repo.toString(), "../../etc").status())
                .isEqualTo(ChangeResolutionStatus.PATH_ESCAPING);
        assertThat(resolver.resolve(repo.toString(), "/etc/passwd").status())
                .isEqualTo(ChangeResolutionStatus.PATH_ESCAPING);
        assertThat(resolver.resolve(repo.toString(), "sub\\dir").status())
                .isEqualTo(ChangeResolutionStatus.PATH_ESCAPING);
    }

    @Test
    void cliStartFailure_shouldBeReportedAsCliFailure() throws IOException {
        Path repo = Files.createDirectories(tempDir.resolve("repo"));
        ChangeResolver resolver = new OpenSpecChangeResolver(
                tempDir.resolve("no-such-openspec").toString(), Duration.ofSeconds(5));

        ChangeResolution resolution = resolver.resolve(repo.toString(), "anything");

        assertThat(resolution.status()).isEqualTo(ChangeResolutionStatus.CLI_FAILURE);
        assertThat(resolution.reason()).contains("failed to start");
    }

    @Test
    void unparseableOutput_shouldBeReportedAsCliFailure() throws IOException {
        Path repo = Files.createDirectories(tempDir.resolve("repo"));
        ChangeResolver resolver = resolver(fakeOpenspec("this is not json"));

        ChangeResolution resolution = resolver.resolve(repo.toString(), "anything");

        assertThat(resolution.status()).isEqualTo(ChangeResolutionStatus.CLI_FAILURE);
        assertThat(resolution.reason()).contains("unparseable");
    }

    @Test
    void nonzeroExit_shouldBeReportedAsCliFailure() throws IOException {
        Path repo = Files.createDirectories(tempDir.resolve("repo"));
        ChangeResolver resolver = resolver(exitOpenspec(3, "boom"));

        ChangeResolution resolution = resolver.resolve(repo.toString(), "anything");

        assertThat(resolution.status()).isEqualTo(ChangeResolutionStatus.CLI_FAILURE);
        assertThat(resolution.reason()).contains("nonzero");
    }

    private ChangeResolver resolver(Path openspec) {
        return new OpenSpecChangeResolver(openspec.toString(), Duration.ofSeconds(10));
    }

    /** Writes an executable fake openspec that prints {@code output} and exits 0. */
    private Path fakeOpenspec(String output) throws IOException {
        Path data = Files.createTempFile(tempDir, "openspec-out", ".json");
        Files.writeString(data, output);
        return writeScript("#!/bin/sh\ncat '" + data + "'\n");
    }

    /** Writes an executable fake openspec that prints {@code output} and exits {@code exitCode}. */
    private Path exitOpenspec(int exitCode, String output) throws IOException {
        Path data = Files.createTempFile(tempDir, "openspec-out", ".json");
        Files.writeString(data, output);
        return writeScript("#!/bin/sh\ncat '" + data + "'\nexit " + exitCode + "\n");
    }

    private Path writeScript(String body) throws IOException {
        Path script = Files.createTempFile(tempDir, "fake-openspec", ".sh");
        Files.writeString(script, body);
        if (!script.toFile().setExecutable(true)) {
            throw new IllegalStateException("could not make fake openspec executable");
        }
        return script;
    }
}
