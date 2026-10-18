package com.hydroyura.agents.piflow.workflow.application;

import java.util.Objects;

/**
 * Result of a deterministic verification attempt.
 *
 * @param status    the four-valued outcome of the attempt
 * @param report    bounded diagnostic text (command output and/or exit code, or the reason text)
 * @param truncated whether {@code report} was truncated at the report size bound
 */
public record VerificationResult(VerificationStatus status, String report, boolean truncated) {

    public VerificationResult {
        status = Objects.requireNonNull(status, "status must not be null");
        report = Objects.requireNonNull(report, "report must not be null");
    }

    /** A validly configured command ran and exited {@code 0} within the timeout. */
    public static VerificationResult passed(String report, boolean truncated) {
        return new VerificationResult(VerificationStatus.PASSED, report, truncated);
    }

    /** A validly configured command ran to completion within the timeout and exited nonzero. */
    public static VerificationResult failed(String report, boolean truncated) {
        return new VerificationResult(VerificationStatus.FAILED, report, truncated);
    }

    /** No verification configuration file exists for the target repository. */
    public static VerificationResult notConfigured(String reason) {
        return new VerificationResult(VerificationStatus.NOT_CONFIGURED, reason, false);
    }

    /**
     * Verification could not meaningfully run: the configuration file was invalid/unreadable,
     * the command could not be started, or the command did not complete within the timeout.
     */
    public static VerificationResult infrastructureError(String reason) {
        return new VerificationResult(VerificationStatus.INFRASTRUCTURE_ERROR, reason, false);
    }
}
