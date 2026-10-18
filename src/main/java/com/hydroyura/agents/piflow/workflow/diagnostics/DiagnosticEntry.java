package com.hydroyura.agents.piflow.workflow.diagnostics;

import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable record of one stage attempt for diagnostics and reporting.
 *
 * <p>The reason string is built only from an explicit allow-list of fields (category, an optional
 * status/exit code, and an optional process/protocol-scoped diagnostic text). The {@link #of}
 * factory deliberately has no parameter through which an agent prompt, agent final text, or a
 * {@code @ConfigurationProperties} object (which could carry a credential) can enter the reason.
 *
 * @param stage           the workflow stage the attempt ran
 * @param attemptSequence the 1-based attempt sequence number, read from plan 15's attempt history
 * @param startedAt       attempt start timestamp
 * @param duration        attempt wall-clock duration (non-negative)
 * @param category        closed, machine-readable outcome classification
 * @param reason          short, safe reason string (prompt/response/secret-free)
 * @param reasonTruncated whether {@code reason} was truncated at the configured reason-length cap
 */
public record DiagnosticEntry(
        WorkflowStage stage,
        int attemptSequence,
        Instant startedAt,
        Duration duration,
        DiagnosticCategory category,
        String reason,
        boolean reasonTruncated) {

    public DiagnosticEntry {
        stage = Objects.requireNonNull(stage, "stage must not be null");
        startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        duration = Objects.requireNonNull(duration, "duration must not be null");
        category = Objects.requireNonNull(category, "category must not be null");
        reason = Objects.requireNonNull(reason, "reason must not be null");
        if (attemptSequence < 1) {
            throw new IllegalArgumentException("attemptSequence must be positive");
        }
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
    }

    /**
     * Builds a diagnostic entry from only allow-listed primitive/enum values, applying the
     * configured reason-length cap. Never accepts a {@code StageOutcome}, {@code PiRunResult},
     * prompt, final text, or configuration-properties object.
     *
     * @param statusOrExitCode optional status/exit code to include in the reason (may be null)
     * @param diagnosticText   optional process/protocol-scoped diagnostic text (may be null)
     * @param maxReasonLength  the reason-length cap to apply
     */
    public static DiagnosticEntry of(
            WorkflowStage stage,
            int attemptSequence,
            Instant startedAt,
            Duration duration,
            DiagnosticCategory category,
            String statusOrExitCode,
            String diagnosticText,
            int maxReasonLength) {
        if (maxReasonLength < 1) {
            throw new IllegalArgumentException("maxReasonLength must be positive");
        }
        String reason = buildReason(category, statusOrExitCode, diagnosticText);
        boolean truncated = reason.length() > maxReasonLength;
        if (truncated) {
            reason = reason.substring(0, maxReasonLength);
        }
        return new DiagnosticEntry(stage, attemptSequence, startedAt, duration, category, reason, truncated);
    }

    private static String buildReason(DiagnosticCategory category, String statusOrExitCode, String diagnosticText) {
        StringBuilder sb = new StringBuilder(category.name());
        if (statusOrExitCode != null && !statusOrExitCode.isBlank()) {
            sb.append(" status=").append(statusOrExitCode);
        }
        if (diagnosticText != null && !diagnosticText.isBlank()) {
            sb.append(" diagnostics=").append(diagnosticText);
        }
        return sb.toString();
    }
}
