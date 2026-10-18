package com.hydroyura.agents.piflow.workflow.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.agents.piflow.workflow.api.StageOutcome;
import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStatus;
import com.hydroyura.agents.piflow.workflow.diagnostics.DiagnosticCategory;
import com.hydroyura.agents.piflow.workflow.diagnostics.DiagnosticEntry;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class WorkflowTest {

    @Test
    void create_shouldStartAtTaskSetupAndInProgress() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");

        assertThat(workflow.getCurrentStage()).isEqualTo(WorkflowStage.TASK_SETUP);
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
        assertThat(workflow.outcomes()).isEmpty();
    }

    @Test
    void recordOutcome_moveTo_markAwaitingUser_shouldUpdateState() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");
        workflow.recordOutcome(StageOutcome.success(WorkflowStage.TASK_SETUP, ""));
        workflow.moveTo(WorkflowStage.PROJECT_ANALYSIS);
        workflow.markAwaitingUser();

        assertThat(workflow.outcomes()).hasSize(1);
        assertThat(workflow.getCurrentStage()).isEqualTo(WorkflowStage.PROJECT_ANALYSIS);
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.AWAITING_USER);
        assertThat(workflow.isTerminal()).isFalse();
    }

    @Test
    void requireAwaitingUserAt_shouldPassAtExpectedStageAndAwaitingUser() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");
        workflow.moveTo(WorkflowStage.VALIDATION_APPROVAL);
        workflow.markAwaitingUser();

        assertThatCode(() -> workflow.requireAwaitingUserAt(WorkflowStage.VALIDATION_APPROVAL))
                .doesNotThrowAnyException();
    }

    @Test
    void requireAwaitingUserAt_shouldRejectCorrectStageButInProgressStatus() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");
        workflow.moveTo(WorkflowStage.VALIDATION_APPROVAL);
        workflow.markInProgress();

        assertThatThrownBy(() -> workflow.requireAwaitingUserAt(WorkflowStage.VALIDATION_APPROVAL))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requireAwaitingUserAt_shouldRejectCorrectStageButTerminalStatus() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");
        workflow.moveTo(WorkflowStage.VALIDATION_APPROVAL);
        workflow.markCancelled();

        assertThatThrownBy(() -> workflow.requireAwaitingUserAt(WorkflowStage.VALIDATION_APPROVAL))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recordPendingFeedback_consume_shouldReturnValueThenResetToEmpty() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");
        workflow.recordPendingFeedback("the error handling section is missing");

        assertThat(workflow.consumePendingFeedback()).isEqualTo("the error handling section is missing");
        assertThat(workflow.consumePendingFeedback()).isEmpty();
    }

    @Test
    void recordPendingFeedback_emptyString_shouldBeAcceptedAndConsumedAsEmpty() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");

        assertThatCode(() -> workflow.recordPendingFeedback(""))
                .doesNotThrowAnyException();

        assertThat(workflow.consumePendingFeedback()).isEmpty();
    }

    @Test
    void changeId_shouldBeAbsentUntilBoundAndImmutableOnceBound() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");

        assertThat(workflow.getChangeId()).isEmpty();

        workflow.bindChange("change-a");

        assertThat(workflow.getChangeId()).contains("change-a");
        assertThatCode(() -> workflow.bindChange("change-a")).doesNotThrowAnyException();
        assertThat(workflow.getChangeId()).contains("change-a");

        assertThatThrownBy(() -> workflow.bindChange("change-b"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("change-a")
                .hasMessageContaining("change-b");
        assertThat(workflow.getChangeId()).contains("change-a");
    }

    @Test
    void approvalFingerprint_shouldBeAbsentUntilRecordedAndOverwritable() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");

        assertThat(workflow.getApprovalFingerprint()).isEmpty();

        workflow.recordApprovalFingerprint("fingerprint-1");
        assertThat(workflow.getApprovalFingerprint()).contains("fingerprint-1");

        workflow.recordApprovalFingerprint("fingerprint-2");
        assertThat(workflow.getApprovalFingerprint()).contains("fingerprint-2");
    }

    @Test
    void markPaused_shouldSetPausedStatus_andPausedIsNotTerminal() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");

        workflow.markPaused();

        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.PAUSED);
        assertThat(workflow.isTerminal()).isFalse();
    }

    @Test
    void pausedAndAwaitingUser_shouldBeDistinctValues() {
        assertThat(WorkflowStatus.PAUSED).isNotEqualTo(WorkflowStatus.AWAITING_USER);
    }

    @Test
    void retryCounters_shouldIncrementAndTechnicalRetryResetsOnMoveTo() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");

        assertThat(workflow.technicalRetryCount()).isZero();
        assertThat(workflow.contentCorrectionCount()).isZero();

        workflow.incrementTechnicalRetryCount();
        workflow.incrementTechnicalRetryCount();
        workflow.incrementContentCorrectionCount();

        assertThat(workflow.technicalRetryCount()).isEqualTo(2);
        assertThat(workflow.contentCorrectionCount()).isEqualTo(1);

        workflow.moveTo(WorkflowStage.CHANGE_PROPOSAL);

        assertThat(workflow.technicalRetryCount()).isZero();
        assertThat(workflow.contentCorrectionCount()).isEqualTo(1);
    }

    @Test
    void recordDiagnostic_shouldBeRetrievableViaDiagnosticsAccessor() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");
        DiagnosticEntry entry = DiagnosticEntry.of(WorkflowStage.PROJECT_ANALYSIS, 1,
                Instant.now(), Duration.ofMillis(1), DiagnosticCategory.SUCCESS, null, null, 500);

        workflow.recordDiagnostic(entry);

        assertThat(workflow.diagnostics().entries()).containsExactly(entry);
    }
}
