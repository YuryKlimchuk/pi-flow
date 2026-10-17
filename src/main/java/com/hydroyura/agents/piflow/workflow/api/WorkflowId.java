package com.hydroyura.agents.piflow.workflow.api;

import java.util.Objects;
import java.util.UUID;

/** Identity of a workflow run. */
public record WorkflowId(UUID value) {

    public WorkflowId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static WorkflowId generate() {
        return new WorkflowId(UUID.randomUUID());
    }
}
