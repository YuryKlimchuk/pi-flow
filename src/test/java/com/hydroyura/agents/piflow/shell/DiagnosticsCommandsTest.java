package com.hydroyura.agents.piflow.shell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hydroyura.agents.piflow.workflow.api.DiagnosticEntryView;
import com.hydroyura.agents.piflow.workflow.api.WorkflowDiagnosticsView;
import com.hydroyura.agents.piflow.workflow.api.WorkflowEngine;
import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagnosticsCommandsTest {

    @Test
    void diagnostics_knownWorkflow_rendersEntriesInChronologicalOrder() {
        WorkflowEngine engine = mock(WorkflowEngine.class);
        WorkflowId id = WorkflowId.generate();
        List<DiagnosticEntryView> entries = List.of(
                new DiagnosticEntryView(WorkflowStage.PROJECT_ANALYSIS, 1, Instant.parse("2026-10-18T12:00:00Z"),
                        Duration.ofMillis(5), "SUCCESS", "SUCCESS", false),
                new DiagnosticEntryView(WorkflowStage.REVIEW, 1, Instant.parse("2026-10-18T12:00:01Z"),
                        Duration.ofMillis(7), "TECHNICAL_FAILURE", "TECHNICAL_FAILURE", false));
        when(engine.diagnostics(id)).thenReturn(new WorkflowDiagnosticsView(id, false, entries));

        DiagnosticsCommands commands = new DiagnosticsCommands(engine);
        String output = commands.diagnostics(id.value().toString());

        assertThat(output).contains(
                "stage=PROJECT_ANALYSIS attempt=1 duration=5ms category=SUCCESS reason=SUCCESS",
                "stage=REVIEW attempt=1 duration=7ms category=TECHNICAL_FAILURE reason=TECHNICAL_FAILURE");
        assertThat(output.indexOf("PROJECT_ANALYSIS")).isLessThan(output.indexOf("REVIEW"));
    }

    @Test
    void diagnostics_withDroppedEntries_rendersTrailingNote() {
        WorkflowEngine engine = mock(WorkflowEngine.class);
        WorkflowId id = WorkflowId.generate();
        when(engine.diagnostics(id)).thenReturn(new WorkflowDiagnosticsView(id, true, List.of()));

        DiagnosticsCommands commands = new DiagnosticsCommands(engine);
        String output = commands.diagnostics(id.value().toString());

        assertThat(output).contains("(older entries dropped)");
    }

    @Test
    void diagnostics_unknownWorkflow_reportsUnknownMessage() {
        WorkflowEngine engine = mock(WorkflowEngine.class);
        WorkflowId id = WorkflowId.generate();
        when(engine.diagnostics(id)).thenThrow(new IllegalStateException("Unknown workflow: " + id.value()));

        DiagnosticsCommands commands = new DiagnosticsCommands(engine);
        String output = commands.diagnostics(id.value().toString());

        assertThat(output).isEqualTo("unknown workflow: " + id.value());
    }
}
