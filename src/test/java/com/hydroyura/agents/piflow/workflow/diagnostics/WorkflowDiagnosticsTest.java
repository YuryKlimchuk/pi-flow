package com.hydroyura.agents.piflow.workflow.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.agents.piflow.workflow.api.StageOutcome;
import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class WorkflowDiagnosticsTest {

    @Test
    void record_withinCap_retainsAllEntriesWithoutDropFlag() {
        WorkflowDiagnostics diagnostics = new WorkflowDiagnostics(new DiagnosticsProperties(3, 500));
        diagnostics.record(entry(1));
        diagnostics.record(entry(2));
        diagnostics.record(entry(3));

        assertThat(diagnostics.entries()).hasSize(3);
        assertThat(diagnostics.entriesDropped()).isFalse();
    }

    @Test
    void record_beyondCap_dropsOldestAndSetsDropFlag() {
        WorkflowDiagnostics diagnostics = new WorkflowDiagnostics(new DiagnosticsProperties(2, 500));
        diagnostics.record(entry(1));
        diagnostics.record(entry(2));
        diagnostics.record(entry(3));

        assertThat(diagnostics.entries()).hasSize(2);
        assertThat(diagnostics.entries().getFirst().attemptSequence()).isEqualTo(2);
        assertThat(diagnostics.entries().getLast().attemptSequence()).isEqualTo(3);
        assertThat(diagnostics.entriesDropped()).isTrue();
    }

    @Test
    void reason_withinCap_storedUnchangedAndNotTruncated() {
        DiagnosticEntry entry = DiagnosticEntry.of(WorkflowStage.PROJECT_ANALYSIS, 1,
                Instant.now(), Duration.ofMillis(1), DiagnosticCategory.SUCCESS, null,
                "short reason", 500);

        assertThat(entry.reason()).isEqualTo("SUCCESS diagnostics=short reason");
        assertThat(entry.reasonTruncated()).isFalse();
    }

    @Test
    void reason_exceedingCap_truncatedAndFlagged() {
        String longText = "x".repeat(100);
        DiagnosticEntry entry = DiagnosticEntry.of(WorkflowStage.PROJECT_ANALYSIS, 1,
                Instant.now(), Duration.ofMillis(1), DiagnosticCategory.SUCCESS, null,
                longText, 10);

        assertThat(entry.reason()).hasSize(10);
        assertThat(entry.reasonTruncated()).isTrue();
    }

    @Test
    void reason_neverContainsPromptFinalTextOrSecrets() {
        String prompt = "FIXTURE-PROMPT-DO-NOT-LOG";
        String finalText = "FIXTURE-FINAL-TEXT-DO-NOT-LOG";
        String secret = "sk-FAKE-API-KEY-12345";

        DiagnosticEntry entry = DiagnosticEntry.of(WorkflowStage.PROJECT_ANALYSIS, 1,
                Instant.now(), Duration.ofMillis(1), DiagnosticCategory.SUCCESS, null,
                "timed out waiting for Pi", 500);

        assertThat(entry.reason())
                .doesNotContain(prompt)
                .doesNotContain(finalText)
                .doesNotContain(secret);
    }

    @Test
    void workflowOutcomes_areUnaffectedByDiagnosticsOverflow() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo",
                new DiagnosticsProperties(2, 500));
        for (int i = 1; i <= 5; i++) {
            workflow.recordOutcome(StageOutcome.success(WorkflowStage.PROJECT_ANALYSIS, "outcome-" + i));
            workflow.recordDiagnostic(entry(i));
        }

        assertThat(workflow.outcomes()).hasSize(5);
        assertThat(workflow.outcomes().stream().map(StageOutcome::output))
                .containsExactly("outcome-1", "outcome-2", "outcome-3", "outcome-4", "outcome-5");
        assertThat(workflow.diagnostics().entries()).hasSize(2);
        assertThat(workflow.diagnostics().entriesDropped()).isTrue();
    }

    private static DiagnosticEntry entry(int sequence) {
        return DiagnosticEntry.of(WorkflowStage.PROJECT_ANALYSIS, sequence,
                Instant.now(), Duration.ofMillis(1), DiagnosticCategory.SUCCESS, null, null, 500);
    }
}
