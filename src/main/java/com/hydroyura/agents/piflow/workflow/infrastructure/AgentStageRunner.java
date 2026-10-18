package com.hydroyura.agents.piflow.workflow.infrastructure;

import com.hydroyura.agents.piflow.agent.api.PiRunResult;
import com.hydroyura.agents.piflow.agent.api.PiRunStatus;
import com.hydroyura.agents.piflow.agent.api.PiRunner;
import com.hydroyura.agents.piflow.workflow.api.StageOutcome;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.application.StageRunner;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Executes agent stages by mapping the stage to a role and invoking Pi under that role. */
@Component
public class AgentStageRunner implements StageRunner {

    private final PiRunner piRunner;

    public AgentStageRunner(PiRunner piRunner) {
        this.piRunner = Objects.requireNonNull(piRunner, "piRunner must not be null");
    }

    @Override
    public StageOutcome run(WorkflowStage stage, Workflow workflow) {
        String agent = agentFor(stage);
        if (agent == null) {
            return StageOutcome.success(stage, "");
        }
        PiRunResult result = piRunner.run(agent, promptFor(stage, workflow), workflow.getTargetRepo());
        return new StageOutcome(stage, result.status() == PiRunStatus.SUCCESS, result.finalText());
    }

    private String agentFor(WorkflowStage stage) {
        return switch (stage) {
            case PROJECT_ANALYSIS -> "analyst";
            case CHANGE_PROPOSAL -> "architect";
            case IMPLEMENTATION -> "developer";
            case REVIEW -> "reviewer";
            default -> null;
        };
    }

    private String promptFor(WorkflowStage stage, Workflow workflow) {
        return switch (stage) {
            case PROJECT_ANALYSIS -> "Analyze the project at " + workflow.getTargetRepo() + " and report findings.";
            case CHANGE_PROPOSAL -> "Prepare OpenSpec change artifacts for the task: " + workflow.getTask();
            case IMPLEMENTATION -> "Implement the approved change for the task: " + workflow.getTask();
            case REVIEW -> "Review the implementation for the task: " + workflow.getTask();
            default -> workflow.getTask();
        };
    }
}
