package com.hydroyura.agents.piflow.workflow.domain;

import com.hydroyura.agents.piflow.workflow.api.StageOutcome;
import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;

/** A workflow run: task, target repository, current stage, status, and stage outcomes. */
@Getter
public class Workflow {

    private final WorkflowId id;
    private final String task;
    private final String targetRepo;
    private WorkflowStage currentStage;
    private WorkflowStatus status;

    @Getter(AccessLevel.NONE)
    private final List<StageOutcome> outcomes = new ArrayList<>();

    public Workflow(WorkflowId id, String task, String targetRepo) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.task = Objects.requireNonNull(task, "task must not be null");
        this.targetRepo = Objects.requireNonNull(targetRepo, "targetRepo must not be null");
        this.currentStage = WorkflowStage.TASK_SETUP;
        this.status = WorkflowStatus.IN_PROGRESS;
    }

    public List<StageOutcome> outcomes() {
        return List.copyOf(outcomes);
    }

    public void recordOutcome(StageOutcome outcome) {
        outcomes.add(Objects.requireNonNull(outcome, "outcome must not be null"));
    }

    public void moveTo(WorkflowStage stage) {
        this.currentStage = Objects.requireNonNull(stage, "stage must not be null");
    }

    public void markInProgress() {
        this.status = WorkflowStatus.IN_PROGRESS;
    }

    public void markAwaitingUser() {
        this.status = WorkflowStatus.AWAITING_USER;
    }

    public void markCompleted() {
        this.status = WorkflowStatus.COMPLETED;
    }

    public void markCancelled() {
        this.status = WorkflowStatus.CANCELLED;
    }

    public void markFailed() {
        this.status = WorkflowStatus.FAILED;
    }

    public boolean isTerminal() {
        return status == WorkflowStatus.COMPLETED
                || status == WorkflowStatus.CANCELLED
                || status == WorkflowStatus.FAILED;
    }
}
