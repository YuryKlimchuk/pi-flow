package com.hydroyura.agents.piflow.workflow.infrastructure;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Parsed verification configuration for a target repository.
 *
 * @param command          argument vector to run, never empty
 * @param timeoutSeconds   positive timeout in seconds
 * @param workingDirectory optional working-directory override; empty means the target repository root
 */
public record VerificationConfig(List<String> command, int timeoutSeconds, Optional<String> workingDirectory) {

    public VerificationConfig {
        command = List.copyOf(Objects.requireNonNull(command, "command must not be null"));
        workingDirectory = Objects.requireNonNull(workingDirectory, "workingDirectory must not be null");
    }
}
