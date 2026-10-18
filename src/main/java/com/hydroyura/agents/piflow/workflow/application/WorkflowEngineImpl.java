package com.hydroyura.agents.piflow.workflow.application;

import com.hydroyura.agents.piflow.workflow.api.ReviewVerdict;
import com.hydroyura.agents.piflow.workflow.api.StageOutcome;
import com.hydroyura.agents.piflow.workflow.api.WorkflowEngine;
import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStatus;
import com.hydroyura.agents.piflow.workflow.api.WorkflowView;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
                workflow.requireAwaitingUserAt(WorkflowStage.VALIDATION_APPROVAL);
                workflow.moveTo(WorkflowStage.IMPLEMENTATION);
                workflow.markInProgress();
                advance(workflow);
            }
            case ACCEPTANCE -> {
                workflow.requireAwaitingUserAt(WorkflowStage.ACCEPTANCE);
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
        workflow.requireAwaitingUserAt(WorkflowStage.VALIDATION_APPROVAL);
        workflow.markCancelled();
        repository.save(workflow);
        return view(workflow);
    }

    @Override
    public WorkflowView requestRevisions(WorkflowId id, String feedback) {
        Workflow workflow = require(id);
        workflow.requireAwaitingUserAt(WorkflowStage.VALIDATION_APPROVAL);
        workflow.recordPendingFeedback(feedback);
        workflow.moveTo(WorkflowStage.CHANGE_PROPOSAL);
        workflow.markInProgress();
        advance(workflow);
        repository.save(workflow);
        return view(workflow);
    }

    @Override
    public WorkflowView requestFurtherWork(WorkflowId id, String feedback) {
        Workflow workflow = require(id);
        workflow.requireAwaitingUserAt(WorkflowStage.ACCEPTANCE);
        workflow.recordPendingFeedback(feedback);
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
                case PROJECT_ANALYSIS, CHANGE_PROPOSAL, IMPLEMENTATION -> {
                    StageContext context = buildStageContext(workflow);
                    StageOutcome outcome;
                    try {
                        outcome = stageRunner.run(stage, workflow, context);
                    } catch (RuntimeException e) {
                        workflow.recordOutcome(StageOutcome.failure(stage, failureMessage(e)));
                        workflow.markFailed();
                        break;
                    }
                    workflow.recordOutcome(outcome);
                    if (outcome.success()) {
                        workflow.moveTo(next(stage));
                    } else {
                        workflow.markFailed();
                    }
                }
                case REVIEW -> {
                    StageContext context = buildStageContext(workflow);
                    StageOutcome outcome;
                    try {
                        outcome = stageRunner.run(stage, workflow, context);
                    } catch (RuntimeException e) {
                        workflow.recordOutcome(StageOutcome.failure(stage, failureMessage(e)));
                        workflow.markFailed();
                        break;
                    }
                    if (outcome.success() && outcome.verdict().isEmpty()) {
                        workflow.recordOutcome(StageOutcome.failure(stage,
                                "REVIEW stage succeeded without a ReviewVerdict (invariant violation)"));
                        workflow.markFailed();
                        break;
                    }
                    workflow.recordOutcome(outcome);
                    if (!outcome.success()) {
                        workflow.markFailed();
                    } else if (outcome.verdict().orElseThrow() == ReviewVerdict.APPROVED) {
                        workflow.moveTo(WorkflowStage.ACCEPTANCE);
                    } else {
                        workflow.moveTo(WorkflowStage.IMPLEMENTATION);
                    }
                }
                case VALIDATION_APPROVAL, ACCEPTANCE -> workflow.markAwaitingUser();
                case VERIFICATION -> {
                    boolean pass;
                    try {
                        pass = verifier.verify(workflow);
                    } catch (RuntimeException e) {
                        workflow.recordOutcome(StageOutcome.failure(stage, failureMessage(e)));
                        workflow.markFailed();
                        break;
                    }
                    workflow.recordOutcome(pass
                            ? StageOutcome.success(stage, "verification passed")
                            : StageOutcome.failure(stage, "verification failed"));
                    workflow.moveTo(pass ? WorkflowStage.REVIEW : WorkflowStage.IMPLEMENTATION);
                }
                case COMPLETED -> workflow.markCompleted();
            }
            repository.save(workflow);
        }
    }

    private StageContext buildStageContext(Workflow workflow) {
        String feedback = workflow.consumePendingFeedback();
        return new StageContext(
                workflow.getTask(),
                workflow.getTargetRepo(),
                latestOutput(workflow, WorkflowStage.PROJECT_ANALYSIS),
                latestOutput(workflow, WorkflowStage.CHANGE_PROPOSAL),
                feedback.isBlank() ? Optional.empty() : Optional.of(feedback));
    }

    private static Optional<String> latestOutput(Workflow workflow, WorkflowStage stage) {
        List<StageOutcome> outcomes = workflow.outcomes();
        for (int i = outcomes.size() - 1; i >= 0; i--) {
            StageOutcome outcome = outcomes.get(i);
            if (outcome.stage() == stage) {
                return Optional.of(outcome.output());
            }
        }
        return Optional.empty();
    }

    private WorkflowStage next(WorkflowStage stage) {
        return switch (stage) {
            case PROJECT_ANALYSIS -> WorkflowStage.CHANGE_PROPOSAL;
            case CHANGE_PROPOSAL -> WorkflowStage.VALIDATION_APPROVAL;
            case IMPLEMENTATION -> WorkflowStage.VERIFICATION;
            default -> throw new IllegalStateException("No next stage after " + stage);
        };
    }

    private static String failureMessage(RuntimeException e) {
        String message = e.getMessage();
        return e.getClass().getSimpleName() + (message == null ? "" : ": " + message);
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
