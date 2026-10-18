package com.hydroyura.agents.piflow.workflow.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads and validates a target repository's verification configuration file.
 *
 * <p>The file is a JSON object with the following fixed schema (the user-facing contract, so it is
 * documented here rather than only in tests):
 *
 * <pre>{@code
 * {
 *   "command": ["./gradlew", "check"],
 *   "timeoutSeconds": 300,
 *   "workingDirectory": "/optional/path"
 * }
 * }</pre>
 *
 * <ul>
 *   <li>{@code command} — required, a non-empty array of strings; the argument vector to run.</li>
 *   <li>{@code timeoutSeconds} — required, a positive integer; the process timeout in seconds.</li>
 *   <li>{@code workingDirectory} — optional string; defaults to the target repository root when absent.</li>
 * </ul>
 *
 * <p>The file is read fresh from disk on every {@link #read(Path)} call — no caching — so a file
 * created or corrected while the application is running is observed by the next read.
 */
public class VerificationConfigReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final VerificationProperties properties;

    public VerificationConfigReader(VerificationProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public VerificationConfigReadResult read(Path targetRepo) {
        Objects.requireNonNull(targetRepo, "targetRepo must not be null");
        Path resolved = resolve(targetRepo);
        if (!Files.exists(resolved)) {
            return new VerificationConfigReadResult.Absent(resolved);
        }

        String text;
        try {
            text = Files.readString(resolved);
        } catch (IOException e) {
            return invalid("Cannot read verification configuration file " + resolved + ": " + e.getMessage());
        }

        JsonNode node;
        try {
            node = MAPPER.readTree(text);
        } catch (JacksonException e) {
            return invalid("Verification configuration file " + resolved + " is not valid JSON: " + e.getMessage());
        }
        if (node == null || !node.isObject()) {
            return invalid("Verification configuration file " + resolved + " is not a JSON object");
        }

        JsonNode commandNode = node.get("command");
        if (commandNode == null) {
            return invalid("missing 'command'");
        }
        if (!commandNode.isArray()) {
            return invalid("'command' must be an array of strings");
        }
        List<String> command = new ArrayList<>();
        for (JsonNode element : commandNode) {
            if (!element.isTextual()) {
                return invalid("'command' must be an array of strings");
            }
            command.add(element.asText());
        }
        if (command.isEmpty()) {
            return invalid("'command' must not be empty");
        }

        JsonNode timeoutNode = node.get("timeoutSeconds");
        if (timeoutNode == null) {
            return invalid("missing 'timeoutSeconds'");
        }
        if (!timeoutNode.isIntegralNumber() || timeoutNode.intValue() <= 0) {
            return invalid("'timeoutSeconds' must be a positive integer");
        }
        int timeoutSeconds = timeoutNode.intValue();

        Optional<String> workingDirectory = Optional.empty();
        JsonNode workingDirectoryNode = node.get("workingDirectory");
        if (workingDirectoryNode != null) {
            if (!workingDirectoryNode.isTextual()) {
                return invalid("'workingDirectory' must be a string");
            }
            workingDirectory = Optional.of(workingDirectoryNode.asText());
        }

        return new VerificationConfigReadResult.Valid(
                new VerificationConfig(command, timeoutSeconds, workingDirectory));
    }

    private Path resolve(Path targetRepo) {
        Path configPath = Path.of(properties.configPath());
        return configPath.isAbsolute() ? configPath : targetRepo.resolve(configPath);
    }

    private static VerificationConfigReadResult.Invalid invalid(String reason) {
        return new VerificationConfigReadResult.Invalid(reason);
    }
}
