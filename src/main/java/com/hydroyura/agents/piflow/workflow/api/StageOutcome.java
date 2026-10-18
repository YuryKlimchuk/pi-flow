package com.hydroyura.agents.piflow.workflow.api;

import com.hydroyura.agents.piflow.workflow.application.VerificationStatus;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Outcome of a single workflow stage. */
public record StageOutcome(
        WorkflowStage stage,
        boolean success,
        String output,
        Optional<ReviewVerdict> verdict,
        Optional<List<String>> findings,
        Optional<VerificationStatus> verificationStatus,
        Optional<String> changeId,
        AttemptKind attemptKind) {

    public StageOutcome {
        stage = Objects.requireNonNull(stage, "stage must not be null");
        output = Objects.requireNonNull(output, "output must not be null");
        verdict = Objects.requireNonNull(verdict, "verdict must not be null");
        findings = Objects.requireNonNull(findings, "findings must not be null");
        verificationStatus = Objects.requireNonNull(verificationStatus, "verificationStatus must not be null");
        changeId = Objects.requireNonNull(changeId, "changeId must not be null");
        attemptKind = Objects.requireNonNull(attemptKind, "attemptKind must not be null");
    }

    public static StageOutcome success(WorkflowStage stage, String output) {
        return new StageOutcome(stage, true, output, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), AttemptKind.INITIAL);
    }

    public static StageOutcome failure(WorkflowStage stage, String output) {
        return new StageOutcome(stage, false, output, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), AttemptKind.INITIAL);
    }

    /** A technically successful {@code REVIEW} outcome carrying a verdict and findings list. */
    public static StageOutcome reviewed(WorkflowStage stage, String output, ReviewVerdict verdict, List<String> findings) {
        Objects.requireNonNull(verdict, "verdict must not be null");
        Objects.requireNonNull(findings, "findings must not be null");
        return new StageOutcome(stage, true, output, Optional.of(verdict), Optional.of(List.copyOf(findings)),
                Optional.empty(), Optional.empty(), AttemptKind.INITIAL);
    }

    /** A {@code VERIFICATION} outcome carrying the four-valued verification status; never populates review fields. */
    public static StageOutcome verified(WorkflowStage stage, String output, VerificationStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        return new StageOutcome(stage, status == VerificationStatus.PASSED, output, Optional.empty(), Optional.empty(),
                Optional.of(status), Optional.empty(), AttemptKind.INITIAL);
    }

    /**
     * A technically successful {@code CHANGE_PROPOSAL} outcome carrying the resolved change identifier
     * the architect created or revised; never populates review or verification fields.
     */
    public static StageOutcome changeProposed(WorkflowStage stage, String output, String changeId) {
        Objects.requireNonNull(changeId, "changeId must not be null");
        return new StageOutcome(stage, true, output, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(changeId), AttemptKind.INITIAL);
    }

    /** A re-run of a technically-failed stage, classified as a technical retry. */
    public static StageOutcome technicalRetry(WorkflowStage stage, String output) {
        return new StageOutcome(stage, false, output, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), AttemptKind.TECHNICAL_RETRY);
    }

    /** A validly-executed stage whose result required another pass, classified as a content correction. */
    public static StageOutcome contentCorrection(WorkflowStage stage, String output) {
        return new StageOutcome(stage, false, output, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), AttemptKind.CONTENT_CORRECTION);
    }

    /** Returns a copy of this outcome carrying the supplied attempt kind, preserving all other fields. */
    public StageOutcome withAttemptKind(AttemptKind kind) {
        Objects.requireNonNull(kind, "kind must not be null");
        return new StageOutcome(stage, success, output, verdict, findings, verificationStatus, changeId, kind);
    }
}
