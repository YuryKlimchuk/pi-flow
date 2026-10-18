package com.hydroyura.agents.piflow.workflow.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class StageOutcomeTest {

    @Test
    void success_shouldDefaultVerdictAndFindingsToEmpty() {
        StageOutcome outcome = StageOutcome.success(WorkflowStage.REVIEW, "ok");

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.verdict()).isEmpty();
        assertThat(outcome.findings()).isEmpty();
    }

    @Test
    void failure_shouldDefaultVerdictAndFindingsToEmpty() {
        StageOutcome outcome = StageOutcome.failure(WorkflowStage.REVIEW, "err");

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.verdict()).isEmpty();
        assertThat(outcome.findings()).isEmpty();
    }

    @Test
    void reviewed_shouldCarryVerdictAndFindings() {
        StageOutcome outcome = StageOutcome.reviewed(
                WorkflowStage.REVIEW,
                "ok",
                ReviewVerdict.CHANGES_REQUIRED,
                List.of("missing null check", "untested branch"));

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.verdict()).contains(ReviewVerdict.CHANGES_REQUIRED);
        assertThat(outcome.findings()).contains(List.of("missing null check", "untested branch"));
    }
}
