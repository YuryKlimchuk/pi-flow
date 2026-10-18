package com.hydroyura.agents.piflow.workflow.application;

import com.hydroyura.agents.piflow.workflow.api.AttemptKind;
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

    /**
     * Purely defensive infinite-loop safety net. It is deliberately much larger than any
     * configured retry/correction budget so it is never the product-facing limit; the
     * {@link RetryProperties} budgets are the only limits the workflow can actually reach.
     */
    private static final int MAX_ADVANCE_STEPS = 10_000;

    private final WorkflowRepository repository;
    private final StageRunner stageRunner;
    private final Verifier verifier;
    private final WorktreeCheck worktreeCheck;
    private final ChangeResolver changeResolver;
    private final ChangeValidator changeValidator;
    private final ArtifactFingerprint fingerprint;
    private final RetryProperties retryProperties;

    public WorkflowEngineImpl(WorkflowRepository repository, StageRunner stageRunner, Verifier verifier,
                              WorktreeCheck worktreeCheck, ChangeResolver changeResolver,
                              ChangeValidator changeValidator, ArtifactFingerprint fingerprint,
                              RetryProperties retryProperties) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.stageRunner = Objects.requireNonNull(stageRunner, "stageRunner must not be null");
        this.verifier = Objects.requireNonNull(verifier, "verifier must not be null");
        this.worktreeCheck = Objects.requireNonNull(worktreeCheck, "worktreeCheck must not be null");
        this.changeResolver = Objects.requireNonNull(changeResolver, "changeResolver must not be null");
        this.changeValidator = Objects.requireNonNull(changeValidator, "changeValidator must not be null");
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        this.retryProperties = Objects.requireNonNull(retryProperties, "retryProperties must not be null");
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
    public WorkflowView retryStage(WorkflowId id, boolean acknowledgePartialChanges) {
        Workflow workflow = require(id);
        if (workflow.isTerminal()) {
            throw new IllegalStateException("Workflow is terminal and cannot be retried: " + workflow.getStatus());
        }
        boolean pausedAtEligibleStage = workflow.getStatus() == WorkflowStatus.PAUSED
                && isRetryEligibleStage(workflow.getCurrentStage());
        boolean notConfiguredPause = isNotConfiguredVerificationPause(workflow);
        if (!pausedAtEligibleStage && !notConfiguredPause) {
            throw new IllegalStateException("Workflow is not in a retryable state: stage "
                    + workflow.getCurrentStage() + ", status " + workflow.getStatus());
        }
        if (isWriteStage(workflow.getCurrentStage()) && !acknowledgePartialChanges) {
            throw new IllegalStateException("Retrying write stage " + workflow.getCurrentStage()
                    + " requires --acknowledge-partial-changes");
        }
        if (workflow.technicalRetryCount() >= retryProperties.maxTechnicalRetries()) {
            throw new IllegalStateException("Technical-retry budget exhausted for stage " + workflow.getCurrentStage());
        }
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
                    recordOutcome(workflow, StageOutcome.success(stage, ""));
                    workflow.moveTo(WorkflowStage.PROJECT_ANALYSIS);
                }
                case PROJECT_ANALYSIS -> {
                    StageContext context = buildStageContext(workflow);
                    StageOutcome outcome;
                    try {
                        outcome = stageRunner.run(stage, workflow, context);
                    } catch (RuntimeException e) {
                        handleTechnicalFailure(workflow, StageOutcome.failure(stage, failureMessage(e)));
                        break;
                    }
                    if (outcome.success()) {
                        recordOutcome(workflow, outcome);
                        workflow.moveTo(WorkflowStage.CHANGE_PROPOSAL);
                    } else {
                        handleTechnicalFailure(workflow, outcome);
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
                        handleTechnicalFailure(workflow, StageOutcome.failure(stage, failureMessage(e)));
                        break;
                    }
                    if (outcome.success() && outcome.verdict().isEmpty()) {
                        handleTechnicalFailure(workflow, StageOutcome.failure(stage,
                                "REVIEW stage succeeded without a ReviewVerdict (invariant violation)"));
                        break;
                    }
                    if (!outcome.success()) {
                        handleTechnicalFailure(workflow, outcome);
                    } else if (outcome.verdict().orElseThrow() == ReviewVerdict.APPROVED) {
                        recordOutcome(workflow, outcome);
                        workflow.moveTo(WorkflowStage.ACCEPTANCE);
                    } else {
                        handleContentCorrection(workflow, outcome);
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
                        handleTechnicalFailure(workflow, StageOutcome.failure(stage, failureMessage(e)));
                        break;
                    }
                    StageOutcome outcome = StageOutcome.verified(stage, result.report(), result.status());
                    switch (result.status()) {
                        case PASSED -> {
                            recordOutcome(workflow, outcome);
                            workflow.moveTo(WorkflowStage.REVIEW);
                        }
                        case FAILED -> handleContentCorrection(workflow, outcome);
                        case NOT_CONFIGURED -> {
                            recordOutcome(workflow, outcome);
                            workflow.markAwaitingUser();
                        }
                        case INFRASTRUCTURE_ERROR -> handleTechnicalFailure(workflow, outcome);
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
            handleTechnicalFailure(workflow, StageOutcome.failure(stage, failureMessage(e)));
            return;
        }
        if (!outcome.success()) {
            handleTechnicalFailure(workflow, outcome);
            return;
        }
        recordOutcome(workflow, outcome);

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
            handleTechnicalFailure(workflow, StageOutcome.failure(stage, failureMessage(e)));
            return;
        }
        if (outcome.success()) {
            recordOutcome(workflow, outcome);
            workflow.moveTo(WorkflowStage.VERIFICATION);
        } else {
            handleTechnicalFailure(workflow, outcome);
        }
    }

    /**
     * Records a technical failure (process crash, timeout, or malformed output) and routes it
     * through the technical-retry budget: {@code PAUSED} while budget remains, {@code FAILED}
     * once the budget is exhausted. The attempt is recorded with {@code INITIAL} kind on the
     * stage's first failure and {@code TECHNICAL_RETRY} on every subsequent failure.
     */
    private void handleTechnicalFailure(Workflow workflow, StageOutcome outcome) {
        recordOutcome(workflow, outcome);
        workflow.incrementTechnicalRetryCount();
        if (workflow.technicalRetryCount() >= retryProperties.maxTechnicalRetries()) {
            workflow.markFailed();
        } else {
            workflow.markPaused();
        }
    }

    /**
     * Records a content-correction loop (a {@code CHANGES_REQUIRED} review verdict or a nonzero-exit
     * verification result) and routes back to {@code IMPLEMENTATION} while the content-correction
     * budget remains, or marks the workflow {@code FAILED} once it is exhausted.
     */
    private void handleContentCorrection(Workflow workflow, StageOutcome outcome) {
        workflow.recordOutcome(outcome.withAttemptKind(AttemptKind.CONTENT_CORRECTION));
        workflow.incrementContentCorrectionCount();
        if (workflow.contentCorrectionCount() >= retryProperties.maxContentCorrections()) {
            workflow.markFailed();
        } else {
            workflow.moveTo(WorkflowStage.IMPLEMENTATION);
        }
    }

    private void recordOutcome(Workflow workflow, StageOutcome outcome) {
        workflow.recordOutcome(outcome.withAttemptKind(currentAttemptKind(workflow)));
    }

    private AttemptKind currentAttemptKind(Workflow workflow) {
        return workflow.technicalRetryCount() > 0 ? AttemptKind.TECHNICAL_RETRY : AttemptKind.INITIAL;
    }

    private static boolean isRetryEligibleStage(WorkflowStage stage) {
        return stage == WorkflowStage.PROJECT_ANALYSIS
                || stage == WorkflowStage.REVIEW
                || stage == WorkflowStage.VERIFICATION
                || stage == WorkflowStage.CHANGE_PROPOSAL
                || stage == WorkflowStage.IMPLEMENTATION;
    }

    private static boolean isWriteStage(WorkflowStage stage) {
        return stage == WorkflowStage.CHANGE_PROPOSAL || stage == WorkflowStage.IMPLEMENTATION;
    }

    private boolean isNotConfiguredVerificationPause(Workflow workflow) {
        if (workflow.getCurrentStage() != WorkflowStage.VERIFICATION
                || workflow.getStatus() != WorkflowStatus.AWAITING_USER) {
            return false;
        }
        return lastOutcome(workflow)
                .flatMap(StageOutcome::verificationStatus)
                .filter(status -> status == VerificationStatus.NOT_CONFIGURED)
                .isPresent();
    }

    private static Optional<StageOutcome> lastOutcome(Workflow workflow) {
        List<StageOutcome> outcomes = workflow.outcomes();
        return outcomes.isEmpty() ? Optional.empty() : Optional.of(outcomes.get(outcomes.size() - 1));
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
