package com.hydroyura.agents.piflow.workflow.infrastructure;

import com.hydroyura.agents.piflow.workflow.application.ChangeResolution;
import com.hydroyura.agents.piflow.workflow.application.ChangeResolver;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Resolves a candidate change identifier against the OpenSpec CLI's reported active changes.
 *
 * <p>Path-escaping identifiers are rejected with a pure string-shape check before any CLI call.
 */
@Component
public class OpenSpecChangeResolver implements ChangeResolver {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String command;
    private final Duration timeout;

    public OpenSpecChangeResolver() {
        this("openspec", Duration.ofSeconds(30));
    }

    public OpenSpecChangeResolver(String command, Duration timeout) {
        this.command = Objects.requireNonNull(command, "command must not be null");
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
    }

    @Override
    public ChangeResolution resolve(String targetRepo, String changeId) {
        Objects.requireNonNull(targetRepo, "targetRepo must not be null");
        Objects.requireNonNull(changeId, "changeId must not be null");

        if (isPathEscaping(changeId)) {
            return ChangeResolution.pathEscaping("change identifier is not a plain change name: '" + changeId + "'");
        }

        ExternalCommand.Result result = ExternalCommand.run(
                List.of(command, "list", "--json"), Path.of(targetRepo), timeout);

        if (result.startFailed()) {
            return ChangeResolution.cliFailure("failed to start OpenSpec CLI: " + result.output());
        }
        if (result.timedOut()) {
            return ChangeResolution.cliFailure("OpenSpec CLI timed out");
        }
        if (result.exitCode() != 0) {
            return ChangeResolution.cliFailure("OpenSpec CLI exited nonzero: " + result.output());
        }

        List<String> names = parseActiveChangeNames(result.output());
        if (names == null) {
            return ChangeResolution.cliFailure("unparseable OpenSpec CLI output: " + result.output());
        }

        List<String> matches = names.stream().filter(changeId::equals).toList();
        if (matches.isEmpty()) {
            return ChangeResolution.notFound("no active change matches '" + changeId + "'");
        }
        if (matches.size() > 1) {
            return ChangeResolution.ambiguous("more than one active change matches '" + changeId + "'");
        }
        return ChangeResolution.resolved(matches.getFirst());
    }

    /** Rejects blank identifiers, path separators, and traversal segments before any CLI call. */
    private static boolean isPathEscaping(String changeId) {
        if (changeId.isBlank()) {
            return true;
        }
        if (changeId.contains("..")) {
            return true;
        }
        return changeId.contains("/") || changeId.contains("\\");
    }

    /** Returns the active change names from {@code openspec list --json}, or {@code null} when unparseable. */
    private static List<String> parseActiveChangeNames(String output) {
        JsonNode root;
        try {
            root = MAPPER.readTree(output);
        } catch (JacksonException e) {
            return null;
        }
        if (root == null || !root.isObject()) {
            return null;
        }
        JsonNode changes = root.get("changes");
        if (changes == null || !changes.isArray()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (JsonNode change : changes) {
            JsonNode name = change.get("name");
            if (name == null || !name.isTextual()) {
                return null;
            }
            names.add(name.asText());
        }
        return names;
    }
}
