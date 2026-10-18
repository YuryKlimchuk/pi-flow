package com.hydroyura.agents.piflow.workflow.application;

import com.hydroyura.agents.piflow.workflow.api.ReviewVerdict;
import com.hydroyura.agents.piflow.workflow.api.StageOutcome;
import com.hydroyura.agents.piflow.workflow.api.WorkflowEngine;
import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStatus;
import com.hydroyura.agents.piflow.workflow.api.WorkflowView;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import java.nio.file.Path;
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
    private final WorktreeCheck worktreeCheck;
    private final ChangeResolver changeResolver;
    private final ChangeValidator changeValidator;
    private final ArtifactFingerprint fingerprint;

    public WorkflowEngineImpl(WorkflowRepository repository, StageRunner stageRunner, Verifier verifier,
                              WorktreeCheck worktreeCheck, ChangeResolver changeResolver,
                              ChangeValidator changeValidator, ArtifactFingerprint fingerprint) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.stageRunner = Objects.requireNonNull(stageRunner, "stageRunner must not be null");
        this.verifier = Objects.requireNonNull(verifier, "verifier must not be null");
        this.worktreeCheck = Objects.requireNonNull(worktreeCheck, "worktreeCheck must not be null");
        this.changeResolver = Objects.requireNonNull(changeResolver, "changeResolver must not be null");
        this.changeValidator = Objects.requireNonNull(changeValidator, "changeValidator must not be null");
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint must not be null");
    }

    @Override
    public WorkflowView start(String task, String targetRepo) {
        if (!worktreeCheck.isGitWorktree(Path.of(targetRepo))) {
            throw new IllegalStateException("Target repository is not a Git worktree: " + targetRepo);
        }
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
                approveAtValidationGate(workflow);
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

    @Override
    public WorkflowView retryVerification(WorkflowId id) {
        Workflow workflow = require(id);
        workflow.requireAwaitingUserAt(WorkflowStage.VERIFICATION);
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
                case PROJECT_ANALYSIS -> {
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
                        workflow.moveTo(WorkflowStage.CHANGE_PROPOSAL);
                    } else {
                        workflow.markFailed();
                    }
                }
                case CHANGE_PROPOSAL -> handleChangeProposal(workflow);
                case IMPLEMENTATION -> handleImplementation(workflow);
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
                case VALIDATION_APPROVAL -> {
                    recordGateFingerprint(workflow);
                    workflow.markAwaitingUser();
                }
                case ACCEPTANCE -> workflow.markAwaitingUser();
                case VERIFICATION -> {
                    VerificationResult result;
                    try {
                        result = verifier.verify(workflow);
                    } catch (RuntimeException e) {
                        workflow.recordOutcome(StageOutcome.failure(stage, failureMessage(e)));
                        workflow.markFailed();
                        break;
                    }
                    workflow.recordOutcome(StageOutcome.verified(stage, result.report(), result.status()));
                    switch (result.status()) {
                        case PASSED -> workflow.moveTo(WorkflowStage.REVIEW);
                        case FAILED -> workflow.moveTo(WorkflowStage.IMPLEMENTATION);
                        case NOT_CONFIGURED, INFRASTRUCTURE_ERROR -> workflow.markAwaitingUser();
                    }
                }
                case COMPLETED -> workflow.markCompleted();
            }
            repository.save(workflow);
        }
    }

    private void handleChangeProposal(Workflow workflow) {
        WorkflowStage stage = WorkflowStage.CHANGE_PROPOSAL;
        StageContext context = buildStageContext(workflow);
        StageOutcome outcome;
        try {
            outcome = stageRunner.run(stage, workflow, context);
        } catch (RuntimeException e) {
            workflow.recordOutcome(StageOutcome.failure(stage, failureMessage(e)));
            workflow.markFailed();
            return;
        }
        workflow.recordOutcome(outcome);

        if (!outcome.success()) {
            workflow.markFailed();
            return;
        }

        String candidate = outcome.changeId().orElse(null);
        if (candidate == null || candidate.isBlank()) {
            workflow.recordOutcome(StageOutcome.failure(stage,
                    "CHANGE_PROPOSAL succeeded without a change identifier"));
            workflow.markFailed();
            return;
        }

        ChangeResolution resolution = changeResolver.resolve(workflow.getTargetRepo(), candidate);
        if (!resolution.isResolved()) {
            workflow.recordOutcome(StageOutcome.failure(stage, resolutionFailureMessage(candidate, resolution)));
            workflow.markFailed();
            return;
        }
        String resolvedId = resolution.changeId().orElseThrow();

        if (workflow.getChangeId().isPresent()) {
            if (!workflow.getChangeId().orElseThrow().equals(resolvedId)) {
                workflow.recordOutcome(StageOutcome.failure(stage,
                        "revision named a different change: bound=" + workflow.getChangeId().orElseThrow()
                                + " proposed=" + resolvedId));
                workflow.markFailed();
                return;
            }
        } else {
            workflow.bindChange(resolvedId);
        }

        ValidationResult validation = changeValidator.validate(workflow.getTargetRepo(), resolvedId);
        if (!validation.passed()) {
            workflow.recordOutcome(StageOutcome.failure(stage, "validation failed: " + validation.reason()));
            workflow.markFailed();
            return;
        }

        workflow.moveTo(WorkflowStage.VALIDATION_APPROVAL);
    }

    private void handleImplementation(Workflow workflow) {
        if (driftDetected(workflow)) {
            workflow.recordOutcome(StageOutcome.failure(WorkflowStage.IMPLEMENTATION,
                    "drift detected before implementation: artifact fingerprint changed since approval"));
            revalidateAndReopen(workflow);
            return;
        }

        WorkflowStage stage = WorkflowStage.IMPLEMENTATION;
        StageContext context = buildStageContext(workflow);
        StageOutcome outcome;
        try {
            outcome = stageRunner.run(stage, workflow, context);
        } catch (RuntimeException e) {
            workflow.recordOutcome(StageOutcome.failure(stage, failureMessage(e)));
            workflow.markFailed();
            return;
        }
        workflow.recordOutcome(outcome);
        if (outcome.success()) {
            workflow.moveTo(WorkflowStage.VERIFICATION);
        } else {
            workflow.markFailed();
        }
    }

    private void approveAtValidationGate(Workflow workflow) {
        String current = fingerprint.compute(changeDir(workflow));
        String recorded = workflow.getApprovalFingerprint().orElseThrow(
                () -> new IllegalStateException("approval gate fingerprint missing"));
        if (current.equals(recorded)) {
            workflow.recordApprovalFingerprint(current);
            workflow.moveTo(WorkflowStage.IMPLEMENTATION);
            workflow.markInProgress();
            advance(workflow);
        } else {
            workflow.recordOutcome(StageOutcome.failure(WorkflowStage.VALIDATION_APPROVAL,
                    "drift detected at approval time: artifact fingerprint changed since the gate opened"));
            revalidateAndReopen(workflow);
        }
    }

    private void revalidateAndReopen(Workflow workflow) {
        String changeId = workflow.getChangeId().orElseThrow(
                () -> new IllegalStateException("cannot revalidate without a bound change"));
        ValidationResult validation = changeValidator.validate(workflow.getTargetRepo(), changeId);
        if (validation.passed()) {
            recordGateFingerprint(workflow);
            workflow.moveTo(WorkflowStage.VALIDATION_APPROVAL);
            workflow.markAwaitingUser();
        } else {
            workflow.recordOutcome(StageOutcome.failure(workflow.getCurrentStage(),
                    "drift revalidation failed: " + validation.reason()));
            workflow.moveTo(WorkflowStage.CHANGE_PROPOSAL);
            workflow.markFailed();
        }
    }

    private void recordGateFingerprint(Workflow workflow) {
        workflow.recordApprovalFingerprint(fingerprint.compute(changeDir(workflow)));
    }

    private boolean driftDetected(Workflow workflow) {
        String current = fingerprint.compute(changeDir(workflow));
        String recorded = workflow.getApprovalFingerprint().orElseThrow(
                () -> new IllegalStateException("approval fingerprint missing before implementation"));
        return !current.equals(recorded);
    }

    private Path changeDir(Workflow workflow) {
        String changeId = workflow.getChangeId().orElseThrow(
                () -> new IllegalStateException("workflow has no bound change"));
        return Path.of(workflow.getTargetRepo(), "openspec", "changes", changeId);
    }

    private static String resolutionFailureMessage(String candidate, ChangeResolution resolution) {
        return switch (resolution.status()) {
            case NOT_FOUND -> "change identifier does not resolve to an active change: " + candidate;
            case AMBIGUOUS -> "change identifier is ambiguous: " + candidate;
            case PATH_ESCAPING -> resolution.reason();
            case CLI_FAILURE -> "OpenSpec CLI resolution failed: " + resolution.reason();
            case RESOLVED -> throw new IllegalStateException("resolved resolution cannot be a failure");
        };
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
