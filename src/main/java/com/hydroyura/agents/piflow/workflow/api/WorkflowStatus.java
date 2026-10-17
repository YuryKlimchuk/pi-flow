package com.hydroyura.agents.piflow.workflow.api;

/** Overall status of a workflow run. */
public enum WorkflowStatus {
    IN_PROGRESS,
    AWAITING_USER,
    COMPLETED,
    CANCELLED,
    FAILED
}
