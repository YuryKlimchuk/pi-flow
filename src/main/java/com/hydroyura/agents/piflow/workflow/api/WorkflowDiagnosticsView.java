package com.hydroyura.agents.piflow.workflow.api;

import java.util.List;

/** Read model of a workflow's bounded diagnostics projection, exposed to other modules. */
public record WorkflowDiagnosticsView(
        WorkflowId id,
        boolean entriesDropped,
        List<DiagnosticEntryView> entries) {
}
