package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.application.VerificationResult;
import com.hydroyura.agents.piflow.workflow.application.VerificationStatus;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommandVerifierTest {

    @TempDir
    Path tempDir;

    @Test
    void verify_withNoConfigFile_shouldReturnNotConfigured() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("no-config"));

        VerificationResult result = verifier().verify(workflow(targetRepo));

        assertThat(result.status()).isEqualTo(VerificationStatus.NOT_CONFIGURED);
        assertThat(result.report()).contains("No verification configuration file found");
    }

    @Test
    void verify_withInvalidConfig_shouldReturnInfrastructureError() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("invalid-config"));
        writeConfig(targetRepo, "{ not json");

        VerificationResult result = verifier().verify(workflow(targetRepo));

        assertThat(result.status()).isEqualTo(VerificationStatus.INFRASTRUCTURE_ERROR);
        assertThat(result.report()).contains("not valid JSON");
    }

    @Test
    void verify_withNonexistentExecutable_shouldReturnInfrastructureError() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("no-exec"));
        writeConfig(targetRepo, config(List.of(tempDir.resolve("does-not-exist").toString()), 10));

        VerificationResult result = verifier().verify(workflow(targetRepo));

        assertThat(result.status()).isEqualTo(VerificationStatus.INFRASTRUCTURE_ERROR);
        assertThat(result.report()).contains("Failed to start verification command");
    }

    @Test
    void verify_withNonexistentWorkingDirectory_shouldReturnInfrastructureError() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("no-cwd"));
        writeConfig(targetRepo,
                config(List.of("sh", "-c", "exit 0"), 10, tempDir.resolve("missing-dir").toString()));

        VerificationResult result = verifier().verify(workflow(targetRepo));

        assertThat(result.status()).isEqualTo(VerificationStatus.INFRASTRUCTURE_ERROR);
        assertThat(result.report()).contains("Failed to start verification command");
    }

    @Test
    void verify_withExitZero_shouldReturnPassed() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("pass"));
        writeConfig(targetRepo, config(List.of("sh", "-c", "exit 0"), 10));

        VerificationResult result = verifier().verify(workflow(targetRepo));

        assertThat(result.status()).isEqualTo(VerificationStatus.PASSED);
    }

    @Test
    void verify_withExitNonZero_shouldReturnFailed() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("fail"));
        writeConfig(targetRepo, config(List.of("sh", "-c", "exit 1"), 10));

        VerificationResult result = verifier().verify(workflow(targetRepo));

        assertThat(result.status()).isEqualTo(VerificationStatus.FAILED);
    }

    @Test
    void verify_withHangingCommand_shouldReturnInfrastructureErrorNotingTimeout() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("hang"));
        writeConfig(targetRepo, config(List.of("sh", "-c", "sleep 30"), 1));

        VerificationResult result = verifier().verify(workflow(targetRepo));

        assertThat(result.status()).isEqualTo(VerificationStatus.INFRASTRUCTURE_ERROR);
        assertThat(result.status()).isNotEqualTo(VerificationStatus.FAILED);
        assertThat(result.report()).contains("timed out");
    }

    @Test
    void verify_withOutputWithinBound_shouldNotBeTruncated() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("small-output"));
        writeConfig(targetRepo, config(List.of("sh", "-c", "printf 'short output'"), 10));

        VerificationResult result = verifier().verify(workflow(targetRepo));

        assertThat(result.status()).isEqualTo(VerificationStatus.PASSED);
        assertThat(result.truncated()).isFalse();
        assertThat(result.report()).contains("short output");
    }

    @Test
    void verify_withOutputExceedingBound_shouldBeTruncatedAndFlagged() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("big-output"));
        Path script = writeScript("big-output.sh", """
                #!/bin/sh
                dd if=/dev/zero bs=30000 count=1 2>/dev/null | tr '\\000' 'a'
                """);
        writeConfig(targetRepo, config(List.of("sh", script.toString()), 30));

        VerificationResult result = verifier().verify(workflow(targetRepo));

        assertThat(result.status()).isEqualTo(VerificationStatus.PASSED);
        assertThat(result.report().length()).isLessThanOrEqualTo(CommandVerifier.MAX_REPORT_CHARS + "exit code 0 [output truncated]\n".length());
        assertThat(result.truncated()).isTrue();
    }

    @Test
    void verify_withWorkingDirectoryOverride_shouldHonorIt() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("cwd-override-repo"));
        Path override = Files.createDirectories(tempDir.resolve("override-dir"));
        writeConfig(targetRepo, config(List.of("sh", "-c", "pwd"), 10, override.toString()));

        VerificationResult result = verifier().verify(workflow(targetRepo));

        assertThat(result.status()).isEqualTo(VerificationStatus.PASSED);
        assertThat(result.report()).contains(override.toString());
    }

    @Test
    void verify_withoutWorkingDirectoryOverride_shouldUseTargetRepo() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("cwd-default-repo"));
        writeConfig(targetRepo, config(List.of("sh", "-c", "pwd"), 10));

        VerificationResult result = verifier().verify(workflow(targetRepo));

        assertThat(result.status()).isEqualTo(VerificationStatus.PASSED);
        assertThat(result.report()).contains(targetRepo.toString());
    }

    @Test
    void verify_shouldRereadConfigLiveWithoutRestart() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("live-reconfig"));
        CommandVerifier verifier = verifier();

        assertThat(verifier.verify(workflow(targetRepo)).status()).isEqualTo(VerificationStatus.NOT_CONFIGURED);

        writeConfig(targetRepo, config(List.of("sh", "-c", "exit 0"), 10));

        assertThat(verifier.verify(workflow(targetRepo)).status()).isEqualTo(VerificationStatus.PASSED);
    }

    private CommandVerifier verifier() {
        return new CommandVerifier(new VerificationProperties(null));
    }

    private static Workflow workflow(Path targetRepo) {
        return new Workflow(WorkflowId.generate(), "task", targetRepo.toString());
    }

    private void writeConfig(Path targetRepo, String json) throws IOException {
        Path dir = Files.createDirectories(targetRepo.resolve(".pi-flow"));
        Files.writeString(dir.resolve("verify.json"), json);
    }

    private Path writeScript(String name, String body) throws IOException {
        Path script = tempDir.resolve(name);
        Files.writeString(script, body);
        if (!script.toFile().setExecutable(true)) {
            throw new IllegalStateException("could not make " + name + " executable");
        }
        return script;
    }

    private static String config(List<String> command, int timeoutSeconds) {
        return "{\"command\":" + json(command) + ",\"timeoutSeconds\":" + timeoutSeconds + "}";
    }

    private static String config(List<String> command, int timeoutSeconds, String workingDirectory) {
        return "{\"command\":" + json(command) + ",\"timeoutSeconds\":" + timeoutSeconds
                + ",\"workingDirectory\":\"" + workingDirectory + "\"}";
    }

    private static String json(List<String> values) {
        return "[" + values.stream().map(v -> "\"" + v + "\"").collect(Collectors.joining(",")) + "]";
    }
}
