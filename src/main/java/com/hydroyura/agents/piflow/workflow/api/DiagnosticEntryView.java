package com.hydroyura.agents.piflow.workflow.api;

import java.time.Duration;
import java.time.Instant;

/**
 * Read model of a single retained diagnostic entry, exposed to other modules (e.g. the shell).
 *
 * <p>The category is carried as its enum {@code name()} string so the shell never depends on the
 * diagnostics package's internal {@code DiagnosticCategory} enum.
 */
public record DiagnosticEntryView(
        WorkflowStage stage,
        int attemptSequence,
        Instant startedAt,
        Duration duration,
        String category,
        String reason,
        boolean reasonTruncated) {
}
