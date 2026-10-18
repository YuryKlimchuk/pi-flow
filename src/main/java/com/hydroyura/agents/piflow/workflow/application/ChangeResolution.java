package com.hydroyura.agents.piflow.workflow.application;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of resolving a candidate change identifier against a target repository's OpenSpec changes.
 *
 * @param status   the resolution category
 * @param changeId the canonical change identifier, present only when {@code status} is {@code RESOLVED}
 * @param reason   human-readable detail, empty only for a successful resolution
 */
public record ChangeResolution(ChangeResolutionStatus status, Optional<String> changeId, String reason) {

    public ChangeResolution {
        status = Objects.requireNonNull(status, "status must not be null");
        changeId = Objects.requireNonNull(changeId, "changeId must not be null");
        reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public static ChangeResolution resolved(String changeId) {
        return new ChangeResolution(ChangeResolutionStatus.RESOLVED,
                Optional.of(Objects.requireNonNull(changeId, "changeId must not be null")), "");
    }

    public static ChangeResolution notFound(String reason) {
        return new ChangeResolution(ChangeResolutionStatus.NOT_FOUND, Optional.empty(), reason);
    }

    public static ChangeResolution ambiguous(String reason) {
        return new ChangeResolution(ChangeResolutionStatus.AMBIGUOUS, Optional.empty(), reason);
    }

    public static ChangeResolution pathEscaping(String reason) {
        return new ChangeResolution(ChangeResolutionStatus.PATH_ESCAPING, Optional.empty(), reason);
    }

    public static ChangeResolution cliFailure(String reason) {
        return new ChangeResolution(ChangeResolutionStatus.CLI_FAILURE, Optional.empty(), reason);
    }

    public boolean isResolved() {
        return status == ChangeResolutionStatus.RESOLVED;
    }
}
