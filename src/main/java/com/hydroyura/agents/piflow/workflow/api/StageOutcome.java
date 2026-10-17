package com.hydroyura.agents.piflow.workflow.api;

/** Outcome of a single workflow stage. */
public record StageOutcome(WorkflowStage stage, boolean success, String output) {

    public static StageOutcome success(WorkflowStage stage, String output) {
        return new StageOutcome(stage, true, output);
    }

    public static StageOutcome failure(WorkflowStage stage, String output) {
        return new StageOutcome(stage, false, output);
    }
}
