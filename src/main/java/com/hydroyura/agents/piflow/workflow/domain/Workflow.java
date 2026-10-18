package com.hydroyura.agents.piflow.workflow.domain;

import com.hydroyura.agents.piflow.workflow.api.StageOutcome;
import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    @Getter(AccessLevel.NONE)
    private String pendingFeedback = "";

    @Getter(AccessLevel.NONE)
    private String changeId;

    @Getter(AccessLevel.NONE)
    private String approvalFingerprint;

    @Getter(AccessLevel.NONE)
    private int technicalRetryCount = 0;

    @Getter(AccessLevel.NONE)
    private int contentCorrectionCount = 0;

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

    /** Records user-supplied revision/further-work feedback for the next relevant stage run. */
    public void recordPendingFeedback(String feedback) {
        this.pendingFeedback = Objects.requireNonNull(feedback, "feedback must not be null");
    }

    /** The bound OpenSpec change identifier, absent until the first successful binding. */
    public Optional<String> getChangeId() {
        return Optional.ofNullable(changeId);
    }

    /**
     * Binds the workflow permanently to an OpenSpec change identifier.
     *
     * <p>The first assignment records the binding; assigning the same value again is a no-op;
     * assigning a different value throws {@link IllegalStateException}.
     */
    public void bindChange(String changeId) {
        Objects.requireNonNull(changeId, "changeId must not be null");
        if (this.changeId == null) {
            this.changeId = changeId;
        } else if (!this.changeId.equals(changeId)) {
            throw new IllegalStateException(
                    "Workflow is already bound to change '" + this.changeId + "' and cannot be rebound to '"
                            + changeId + "'");
        }
    }

    /** The most recently recorded gate/approval fingerprint, absent until the gate first opens. */
    public Optional<String> getApprovalFingerprint() {
        return Optional.ofNullable(approvalFingerprint);
    }

    /** Records (overwrites) the gate/approval fingerprint. */
    public void recordApprovalFingerprint(String fingerprint) {
        this.approvalFingerprint = Objects.requireNonNull(fingerprint, "fingerprint must not be null");
    }

    /** Returns the pending feedback and clears it in one step; empty string means no feedback. */
    public String consumePendingFeedback() {
        String feedback = pendingFeedback;
        pendingFeedback = "";
        return feedback;
    }

    public void moveTo(WorkflowStage stage) {
        this.currentStage = Objects.requireNonNull(stage, "stage must not be null");
        this.technicalRetryCount = 0;
    }

    public void markInProgress() {
        this.status = WorkflowStatus.IN_PROGRESS;
    }

    public void markAwaitingUser() {
        this.status = WorkflowStatus.AWAITING_USER;
    }

    public void markPaused() {
        this.status = WorkflowStatus.PAUSED;
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

    /** Number of technical-failure attempts recorded for the current stage-attempt-sequence. */
    public int technicalRetryCount() {
        return technicalRetryCount;
    }

    /** Number of content-correction loops recorded for the whole workflow. */
    public int contentCorrectionCount() {
        return contentCorrectionCount;
    }

    public void incrementTechnicalRetryCount() {
        technicalRetryCount++;
    }

    public void incrementContentCorrectionCount() {
        contentCorrectionCount++;
    }

    /**
     * Asserts the workflow is paused at the given stage and awaiting a user decision.
     *
     * @throws IllegalStateException if the workflow is not at the expected stage or is not awaiting the user
     */
    public void requireAwaitingUserAt(WorkflowStage expectedStage) {
        Objects.requireNonNull(expectedStage, "expectedStage must not be null");
        if (currentStage != expectedStage || status != WorkflowStatus.AWAITING_USER) {
            throw new IllegalStateException(
                    "Expected workflow at stage " + expectedStage + " with status AWAITING_USER, but was at stage "
                            + currentStage + " with status " + status);
        }
    }
}
