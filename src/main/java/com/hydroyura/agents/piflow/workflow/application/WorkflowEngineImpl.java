package com.hydroyura.agents.piflow.workflow.application;

import com.hydroyura.agents.piflow.workflow.api.StageOutcome;
import com.hydroyura.agents.piflow.workflow.api.WorkflowEngine;
import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStatus;
import com.hydroyura.agents.piflow.workflow.api.WorkflowView;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Hand-rolled enum state machine driving the pi-flow workflow synchronously. */
@Service
public class WorkflowEngineImpl implements WorkflowEngine {

    private static final int MAX_ADVANCE_STEPS = 50;

    private final WorkflowRepository repository;
    private final StageRunner stageRunner;
    private final Verifier verifier;

    public WorkflowEngineImpl(WorkflowRepository repository, StageRunner stageRunner, Verifier verifier) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.stageRunner = Objects.requireNonNull(stageRunner, "stageRunner must not be null");
        this.verifier = Objects.requireNonNull(verifier, "verifier must not be null");
    }

    @Override
    public WorkflowView start(String task, String targetRepo) {
        Workflow workflow = new Workflow(WorkflowId.generate(), task, targetRepo);
        repository.save(workflow);
        advance(workflow);
        return view(workflow);
    }

    @Override
    public WorkflowView status(WorkflowId id) {
        return view(require(id));
    }

    @Override
    public WorkflowView approve(WorkflowId id) {
        Workflow workflow = require(id);
        switch (workflow.getCurrentStage()) {
            case VALIDATION_APPROVAL -> {
                workflow.moveTo(WorkflowStage.IMPLEMENTATION);
                workflow.markInProgress();
                advance(workflow);
            }
            case ACCEPTANCE -> {
                workflow.moveTo(WorkflowStage.COMPLETED);
                workflow.markCompleted();
            }
            default -> throw new IllegalStateException("Nothing to approve at stage " + workflow.getCurrentStage());
        }
        repository.save(workflow);
        return view(workflow);
    }

    @Override
    public WorkflowView reject(WorkflowId id) {
        Workflow workflow = require(id);
        if (workflow.getCurrentStage() != WorkflowStage.VALIDATION_APPROVAL) {
            throw new IllegalStateException("Reject is only allowed at the proposal gate");
        }
        workflow.markCancelled();
        repository.save(workflow);
        return view(workflow);
    }

    @Override
    public WorkflowView requestRevisions(WorkflowId id) {
        Workflow workflow = require(id);
        if (workflow.getCurrentStage() != WorkflowStage.VALIDATION_APPROVAL) {
            throw new IllegalStateException("Revisions are only allowed at the proposal gate");
        }
        workflow.moveTo(WorkflowStage.CHANGE_PROPOSAL);
        workflow.markInProgress();
        advance(workflow);
        repository.save(workflow);
        return view(workflow);
    }

    @Override
    public WorkflowView requestFurtherWork(WorkflowId id) {
        Workflow workflow = require(id);
        if (workflow.getCurrentStage() != WorkflowStage.ACCEPTANCE) {
            throw new IllegalStateException("Further work is only allowed at the acceptance gate");
        }
        workflow.moveTo(WorkflowStage.IMPLEMENTATION);
        workflow.markInProgress();
        advance(workflow);
        repository.save(workflow);
        return view(workflow);
    }

    private void advance(Workflow workflow) {
        int steps = 0;
        while (workflow.getStatus() == WorkflowStatus.IN_PROGRESS) {
            if (++steps > MAX_ADVANCE_STEPS) {
                workflow.markFailed();
                return;
            }

            WorkflowStage stage = workflow.getCurrentStage();
            switch (stage) {
                case TASK_SETUP -> {
                    workflow.recordOutcome(StageOutcome.success(stage, ""));
                    workflow.moveTo(WorkflowStage.PROJECT_ANALYSIS);
                }
                case PROJECT_ANALYSIS, CHANGE_PROPOSAL, IMPLEMENTATION, REVIEW -> {
                    StageOutcome outcome = stageRunner.run(stage, workflow);
                    workflow.recordOutcome(outcome);
                    if (outcome.success()) {
                        workflow.moveTo(next(stage));
                    } else if (stage == WorkflowStage.REVIEW) {
                        workflow.moveTo(WorkflowStage.IMPLEMENTATION); // corrections
                    } else {
                        workflow.markFailed();
                    }
                }
                case VALIDATION_APPROVAL, ACCEPTANCE -> workflow.markAwaitingUser();
                case VERIFICATION -> {
                    boolean pass = verifier.verify(workflow);
                    workflow.recordOutcome(new StageOutcome(stage, pass,
                            pass ? "verification passed" : "verification failed"));
                    workflow.moveTo(pass ? WorkflowStage.REVIEW : WorkflowStage.IMPLEMENTATION);
                }
                case COMPLETED -> workflow.markCompleted();
            }
            repository.save(workflow);
        }
    }

    private WorkflowStage next(WorkflowStage stage) {
        return switch (stage) {
            case PROJECT_ANALYSIS -> WorkflowStage.CHANGE_PROPOSAL;
            case CHANGE_PROPOSAL -> WorkflowStage.VALIDATION_APPROVAL;
            case IMPLEMENTATION -> WorkflowStage.VERIFICATION;
            case REVIEW -> WorkflowStage.ACCEPTANCE;
            default -> throw new IllegalStateException("No next stage after " + stage);
        };
    }

    private Workflow require(WorkflowId id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Unknown workflow: " + id.value()));
    }

    private WorkflowView view(Workflow workflow) {
        return new WorkflowView(workflow.getId(), workflow.getTask(), workflow.getTargetRepo(),
                workflow.getCurrentStage(), workflow.getStatus(), workflow.outcomes());
    }
}
