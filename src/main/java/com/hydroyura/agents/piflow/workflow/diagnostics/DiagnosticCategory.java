package com.hydroyura.agents.piflow.workflow.diagnostics;

/**
 * Closed, machine-readable classification of a single stage attempt's outcome.
 *
 * <p>This enum is local to the diagnostics capability and is mapped from the orchestrator's
 * existing outcome shapes ({@code ReviewVerdict}, {@code VerificationStatus}, and
 * {@code AttemptKind}); it does not depend on {@code StageOutcome} or {@code PiRunResult}.
 *
 * <p>{@link #TIMEOUT} is part of the closed set but is not currently producible from the
 * {@code StageOutcome} shape the orchestrator records: plan 11's {@code PiRunStatus.TIMEOUT}
 * is absorbed into a plain failed outcome before it reaches the workflow engine.
 */
public enum DiagnosticCategory {
    SUCCESS,
    TECHNICAL_FAILURE,
    CONTENT_CORRECTION,
    TIMEOUT,
    NOT_CONFIGURED
}
