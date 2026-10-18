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
        Optional<VerificationStatus> verificationStatus) {

    public StageOutcome {
        stage = Objects.requireNonNull(stage, "stage must not be null");
        output = Objects.requireNonNull(output, "output must not be null");
        verdict = Objects.requireNonNull(verdict, "verdict must not be null");
        findings = Objects.requireNonNull(findings, "findings must not be null");
        verificationStatus = Objects.requireNonNull(verificationStatus, "verificationStatus must not be null");
    }

    public static StageOutcome success(WorkflowStage stage, String output) {
        return new StageOutcome(stage, true, output, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static StageOutcome failure(WorkflowStage stage, String output) {
        return new StageOutcome(stage, false, output, Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** A technically successful {@code REVIEW} outcome carrying a verdict and findings list. */
    public static StageOutcome reviewed(WorkflowStage stage, String output, ReviewVerdict verdict, List<String> findings) {
        Objects.requireNonNull(verdict, "verdict must not be null");
        Objects.requireNonNull(findings, "findings must not be null");
        return new StageOutcome(stage, true, output, Optional.of(verdict), Optional.of(List.copyOf(findings)), Optional.empty());
    }

    /** A {@code VERIFICATION} outcome carrying the four-valued verification status; never populates review fields. */
    public static StageOutcome verified(WorkflowStage stage, String output, VerificationStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        return new StageOutcome(stage, status == VerificationStatus.PASSED, output, Optional.empty(), Optional.empty(),
                Optional.of(status));
    }
}
