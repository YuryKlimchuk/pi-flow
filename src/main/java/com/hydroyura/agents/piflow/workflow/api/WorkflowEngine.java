package com.hydroyura.agents.piflow.workflow.api;

/** Port for driving the pi-flow workflow state machine. */
public interface WorkflowEngine {

    WorkflowView start(String task, String targetRepo);

    WorkflowView status(WorkflowId id);

    WorkflowView approve(WorkflowId id);

    WorkflowView reject(WorkflowId id);

    WorkflowView requestRevisions(WorkflowId id, String feedback);

    WorkflowView requestFurtherWork(WorkflowId id, String feedback);

    WorkflowView retryVerification(WorkflowId id);
}
