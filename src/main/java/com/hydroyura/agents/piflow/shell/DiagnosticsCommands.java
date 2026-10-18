package com.hydroyura.agents.piflow.shell;

import com.hydroyura.agents.piflow.workflow.api.DiagnosticEntryView;
import com.hydroyura.agents.piflow.workflow.api.WorkflowDiagnosticsView;
import com.hydroyura.agents.piflow.workflow.api.WorkflowEngine;
import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import java.util.UUID;
import org.springframework.shell.core.command.annotation.Argument;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.CommandGroup;

/** Reporting-only shell command rendering a workflow's retained diagnostic history as text. */
@CommandGroup
public class DiagnosticsCommands {

    private final WorkflowEngine engine;

    public DiagnosticsCommands(WorkflowEngine engine) {
        this.engine = engine;
    }

    @Command(name = "flow-diagnostics", description = "Show a workflow's recorded stage-attempt diagnostics")
    public String diagnostics(@Argument(index = 0, description = "Workflow id") String id) {
        WorkflowId workflowId;
        try {
            workflowId = new WorkflowId(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return "unknown workflow: " + id;
        }
        try {
            return render(engine.diagnostics(workflowId));
        } catch (IllegalStateException e) {
            return "unknown workflow: " + id;
        }
    }

    private String render(WorkflowDiagnosticsView view) {
        StringBuilder sb = new StringBuilder();
        for (DiagnosticEntryView entry : view.entries()) {
            sb.append("stage=").append(entry.stage())
                    .append(" attempt=").append(entry.attemptSequence())
                    .append(" duration=").append(entry.duration().toMillis()).append("ms")
                    .append(" category=").append(entry.category())
                    .append(" reason=").append(entry.reason())
                    .append('\n');
        }
        if (view.entriesDropped()) {
            sb.append("(older entries dropped)\n");
        }
        return sb.toString();
    }
}
