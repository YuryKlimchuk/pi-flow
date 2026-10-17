package com.hydroyura.agents.piflow.workflow.api;

/** The stages of the pi-flow workflow. */
public enum WorkflowStage {
    TASK_SETUP,
    PROJECT_ANALYSIS,
    CHANGE_PROPOSAL,
    VALIDATION_APPROVAL,
    IMPLEMENTATION,
    VERIFICATION,
    REVIEW,
    ACCEPTANCE,
    COMPLETED
}
