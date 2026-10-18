package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VerificationConfigReaderTest {

    @TempDir
    Path tempDir;

    private final VerificationConfigReader reader =
            new VerificationConfigReader(new VerificationProperties(null));

    @Test
    void read_withAbsentFile_shouldReturnAbsent() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("absent-repo"));

        VerificationConfigReadResult result = reader.read(targetRepo);

        assertThat(result).isInstanceOf(VerificationConfigReadResult.Absent.class);
        VerificationConfigReadResult.Absent absent = (VerificationConfigReadResult.Absent) result;
        assertThat(absent.resolvedPath().toString()).endsWith(".pi-flow/verify.json");
    }

    @Test
    void read_withValidFileWithoutWorkingDirectory_shouldParse() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("valid-repo"));
        writeConfig(targetRepo, """
                {"command": ["./check.sh"], "timeoutSeconds": 60}
                """);

        VerificationConfigReadResult result = reader.read(targetRepo);

        assertThat(result).isInstanceOf(VerificationConfigReadResult.Valid.class);
        VerificationConfig config = ((VerificationConfigReadResult.Valid) result).config();
        assertThat(config.command()).containsExactly("./check.sh");
        assertThat(config.timeoutSeconds()).isEqualTo(60);
        assertThat(config.workingDirectory()).isEmpty();
    }

    @Test
    void read_withValidFileAndWorkingDirectory_shouldParse() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("valid-cwd-repo"));
        writeConfig(targetRepo, """
                {"command": ["/opt/check.sh"], "timeoutSeconds": 10, "workingDirectory": "/tmp/alt"}
                """);

        VerificationConfigReadResult result = reader.read(targetRepo);

        assertThat(result).isInstanceOf(VerificationConfigReadResult.Valid.class);
        VerificationConfig config = ((VerificationConfigReadResult.Valid) result).config();
        assertThat(config.workingDirectory()).contains("/tmp/alt");
    }

    @Test
    void read_withUnreadableFile_shouldReturnInvalidCannotRead() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("unreadable-repo"));
        Path directory = Files.createDirectories(targetRepo.resolve(".pi-flow/verify.json"));

        VerificationConfigReader dirReader = new VerificationConfigReader(
                new VerificationProperties(directory.toString()));
        VerificationConfigReadResult result = dirReader.read(targetRepo);

        assertInvalid(result).contains("Cannot read");
    }

    @Test
    void read_withMalformedJson_shouldReturnInvalidJson() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("malformed-repo"));
        writeConfig(targetRepo, "{ not json");

        assertInvalid(reader.read(targetRepo)).contains("not valid JSON");
    }

    @Test
    void read_withJsonArray_shouldReturnInvalidNotObject() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("array-repo"));
        writeConfig(targetRepo, "[1, 2, 3]");

        assertInvalid(reader.read(targetRepo)).contains("not a JSON object");
    }

    @Test
    void read_withMissingCommand_shouldReturnInvalid() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("missing-command-repo"));
        writeConfig(targetRepo, "{\"timeoutSeconds\": 60}");

        assertInvalid(reader.read(targetRepo)).contains("command");
    }

    @Test
    void read_withEmptyCommand_shouldReturnInvalid() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("empty-command-repo"));
        writeConfig(targetRepo, "{\"command\": [], \"timeoutSeconds\": 60}");

        assertInvalid(reader.read(targetRepo)).contains("command").contains("empty");
    }

    @Test
    void read_withNonArrayCommand_shouldReturnInvalid() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("non-array-command-repo"));
        writeConfig(targetRepo, "{\"command\": \"./check.sh\", \"timeoutSeconds\": 60}");

        assertInvalid(reader.read(targetRepo)).contains("command");
    }

    @Test
    void read_withNonStringCommandElement_shouldReturnInvalid() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("non-string-command-repo"));
        writeConfig(targetRepo, "{\"command\": [1], \"timeoutSeconds\": 60}");

        assertInvalid(reader.read(targetRepo)).contains("command");
    }

    @Test
    void read_withMissingTimeoutSeconds_shouldReturnInvalid() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("missing-timeout-repo"));
        writeConfig(targetRepo, "{\"command\": [\"./check.sh\"]}");

        assertInvalid(reader.read(targetRepo)).contains("timeoutSeconds");
    }

    @Test
    void read_withNonPositiveTimeoutSeconds_shouldReturnInvalid() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("nonpositive-timeout-repo"));
        writeConfig(targetRepo, "{\"command\": [\"./check.sh\"], \"timeoutSeconds\": 0}");

        assertInvalid(reader.read(targetRepo)).contains("timeoutSeconds").contains("positive");
    }

    @Test
    void read_withNonIntegerTimeoutSeconds_shouldReturnInvalid() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("noninteger-timeout-repo"));
        writeConfig(targetRepo, "{\"command\": [\"./check.sh\"], \"timeoutSeconds\": \"60\"}");

        assertInvalid(reader.read(targetRepo)).contains("timeoutSeconds").contains("positive");
    }

    @Test
    void read_withNonStringWorkingDirectory_shouldReturnInvalid() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("nonstring-cwd-repo"));
        writeConfig(targetRepo, "{\"command\": [\"./check.sh\"], \"timeoutSeconds\": 60, \"workingDirectory\": 7}");

        assertInvalid(reader.read(targetRepo)).contains("workingDirectory");
    }

    @Test
    void read_shouldRereadFreshOnEveryInvocation() throws IOException {
        Path targetRepo = Files.createDirectories(tempDir.resolve("reread-repo"));
        writeConfig(targetRepo, "{\"command\": [\"./first.sh\"], \"timeoutSeconds\": 60}");

        VerificationConfigReadResult first = reader.read(targetRepo);
        VerificationConfig firstConfig = ((VerificationConfigReadResult.Valid) first).config();
        assertThat(firstConfig.command()).containsExactly("./first.sh");

        writeConfig(targetRepo, "{\"command\": [\"./second.sh\"], \"timeoutSeconds\": 30}");

        VerificationConfigReadResult second = reader.read(targetRepo);
        VerificationConfig secondConfig = ((VerificationConfigReadResult.Valid) second).config();
        assertThat(secondConfig.command()).containsExactly("./second.sh");
        assertThat(secondConfig.timeoutSeconds()).isEqualTo(30);
    }

    private void writeConfig(Path targetRepo, String json) throws IOException {
        Path configDir = Files.createDirectories(targetRepo.resolve(".pi-flow"));
        Files.writeString(configDir.resolve("verify.json"), json);
    }

    private static org.assertj.core.api.AbstractStringAssert<?> assertInvalid(VerificationConfigReadResult result) {
        assertThat(result).isInstanceOf(VerificationConfigReadResult.Invalid.class);
        return assertThat(((VerificationConfigReadResult.Invalid) result).reason());
    }
}
