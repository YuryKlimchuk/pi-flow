package com.hydroyura.agents.piflow.shell;

import com.hydroyura.agents.piflow.workflow.api.StageOutcome;
import com.hydroyura.agents.piflow.workflow.api.WorkflowEngine;
import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.api.WorkflowView;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.core.command.annotation.Argument;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.CommandGroup;

/** Shell commands to drive the pi-flow workflow. */
@CommandGroup
@Slf4j
public class FlowCommands {

    private final WorkflowEngine engine;

    public FlowCommands(WorkflowEngine engine) {
        this.engine = engine;
    }

    @Command(name = "flow-start", description = "Start a workflow for a task against a target repository")
    public String start(@Argument(index = 0, description = "Task description") String task,
                        @Argument(index = 1, description = "Target repository path") String targetRepo) {
        return render(engine.start(task, targetRepo));
    }

    @Command(name = "flow-status", description = "Show a workflow's current stage and status")
    public String status(@Argument(index = 0, description = "Workflow id") String id) {
        return render(engine.status(parse(id)));
    }

    @Command(name = "flow-approve", description = "Approve the proposal, or accept the final result")
    public String approve(@Argument(index = 0, description = "Workflow id") String id) {
        return render(engine.approve(parse(id)));
    }

    @Command(name = "flow-reject", description = "Reject the proposal (cancels the workflow)")
    public String reject(@Argument(index = 0, description = "Workflow id") String id) {
        return render(engine.reject(parse(id)));
    }

    @Command(name = "flow-revise", description = "Request revisions to the proposal")
    public String revise(@Argument(index = 0, description = "Workflow id") String id) {
        return render(engine.requestRevisions(parse(id)));
    }

    @Command(name = "flow-rework", description = "Request further work after review")
    public String rework(@Argument(index = 0, description = "Workflow id") String id) {
        return render(engine.requestFurtherWork(parse(id)));
    }

    private WorkflowId parse(String id) {
        return new WorkflowId(UUID.fromString(id));
    }

    private String render(WorkflowView view) {
        StringBuilder sb = new StringBuilder();
        sb.append("id=").append(view.id().value()).append('\n');
        sb.append("stage=").append(view.currentStage()).append('\n');
        sb.append("status=").append(view.status()).append('\n');
        if (!view.outcomes().isEmpty()) {
            StageOutcome last = view.outcomes().getLast();
            sb.append("lastOutput=").append(last.output()).append('\n');
        }
        return sb.toString();
    }
}
