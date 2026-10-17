package com.hydroyura.agents.piflow.workflow.api;

import java.util.List;

/** Read model of a workflow, exposed to other modules (e.g. the shell). */
public record WorkflowView(
        WorkflowId id,
        String task,
        String targetRepo,
        WorkflowStage currentStage,
        WorkflowStatus status,
        List<StageOutcome> outcomes) {
}
