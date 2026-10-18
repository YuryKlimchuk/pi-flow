package com.hydroyura.agents.piflow.workflow.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.agents.piflow.workflow.application.VerificationStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class StageOutcomeTest {

    @Test
    void success_shouldDefaultVerdictFindingsAndVerificationStatusToEmpty() {
        StageOutcome outcome = StageOutcome.success(WorkflowStage.REVIEW, "ok");

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.verdict()).isEmpty();
        assertThat(outcome.findings()).isEmpty();
        assertThat(outcome.verificationStatus()).isEmpty();
    }

    @Test
    void failure_shouldDefaultVerdictFindingsAndVerificationStatusToEmpty() {
        StageOutcome outcome = StageOutcome.failure(WorkflowStage.REVIEW, "err");

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.verdict()).isEmpty();
        assertThat(outcome.findings()).isEmpty();
        assertThat(outcome.verificationStatus()).isEmpty();
    }

    @Test
    void reviewed_shouldCarryVerdictAndFindingsButNoVerificationStatus() {
        StageOutcome outcome = StageOutcome.reviewed(
                WorkflowStage.REVIEW,
                "ok",
                ReviewVerdict.CHANGES_REQUIRED,
                List.of("missing null check", "untested branch"));

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.verdict()).contains(ReviewVerdict.CHANGES_REQUIRED);
        assertThat(outcome.findings()).contains(List.of("missing null check", "untested branch"));
        assertThat(outcome.verificationStatus()).isEmpty();
    }

    @Test
    void verified_shouldCarryVerificationStatusWithVerdictAndFindingsEmpty() {
        StageOutcome outcome = StageOutcome.verified(WorkflowStage.VERIFICATION, "report", VerificationStatus.PASSED);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.verificationStatus()).contains(VerificationStatus.PASSED);
        assertThat(outcome.verdict()).isEmpty();
        assertThat(outcome.findings()).isEmpty();
    }

    @Test
    void verified_nonPassedStatus_shouldBeRecordedAsFailure() {
        StageOutcome outcome = StageOutcome.verified(WorkflowStage.VERIFICATION, "report", VerificationStatus.FAILED);

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.verificationStatus()).contains(VerificationStatus.FAILED);
        assertThat(outcome.verdict()).isEmpty();
        assertThat(outcome.findings()).isEmpty();
    }

    @Test
    void defaultFactories_shouldProduceInitialAttemptKind() {
        assertThat(StageOutcome.success(WorkflowStage.REVIEW, "ok").attemptKind()).isEqualTo(AttemptKind.INITIAL);
        assertThat(StageOutcome.failure(WorkflowStage.REVIEW, "err").attemptKind()).isEqualTo(AttemptKind.INITIAL);
        assertThat(StageOutcome.reviewed(WorkflowStage.REVIEW, "ok", ReviewVerdict.APPROVED, List.of()).attemptKind())
                .isEqualTo(AttemptKind.INITIAL);
        assertThat(StageOutcome.verified(WorkflowStage.VERIFICATION, "ok", VerificationStatus.PASSED).attemptKind())
                .isEqualTo(AttemptKind.INITIAL);
        assertThat(StageOutcome.changeProposed(WorkflowStage.CHANGE_PROPOSAL, "ok", "change-a").attemptKind())
                .isEqualTo(AttemptKind.INITIAL);
    }

    @Test
    void technicalRetry_shouldProduceTechnicalRetryAttemptKind() {
        StageOutcome outcome = StageOutcome.technicalRetry(WorkflowStage.REVIEW, "retry");

        assertThat(outcome.attemptKind()).isEqualTo(AttemptKind.TECHNICAL_RETRY);
        assertThat(outcome.success()).isFalse();
    }

    @Test
    void contentCorrection_shouldProduceContentCorrectionAttemptKind() {
        StageOutcome outcome = StageOutcome.contentCorrection(WorkflowStage.REVIEW, "correction");

        assertThat(outcome.attemptKind()).isEqualTo(AttemptKind.CONTENT_CORRECTION);
        assertThat(outcome.success()).isFalse();
    }

    @Test
    void withAttemptKind_shouldPreserveAllOtherFields() {
        StageOutcome outcome = StageOutcome.reviewed(
                WorkflowStage.REVIEW, "ok", ReviewVerdict.CHANGES_REQUIRED, List.of("finding"));

        StageOutcome reclassified = outcome.withAttemptKind(AttemptKind.CONTENT_CORRECTION);

        assertThat(reclassified.attemptKind()).isEqualTo(AttemptKind.CONTENT_CORRECTION);
        assertThat(reclassified.stage()).isEqualTo(outcome.stage());
        assertThat(reclassified.success()).isEqualTo(outcome.success());
        assertThat(reclassified.output()).isEqualTo(outcome.output());
        assertThat(reclassified.verdict()).isEqualTo(outcome.verdict());
        assertThat(reclassified.findings()).isEqualTo(outcome.findings());
        assertThat(reclassified.verificationStatus()).isEqualTo(outcome.verificationStatus());
        assertThat(reclassified.changeId()).isEqualTo(outcome.changeId());
    }
}
