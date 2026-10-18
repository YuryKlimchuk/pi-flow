package com.hydroyura.agents.piflow.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.agents.piflow.workflow.api.ReviewVerdict;
import com.hydroyura.agents.piflow.workflow.api.StageOutcome;
import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStatus;
import com.hydroyura.agents.piflow.workflow.api.WorkflowView;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import com.hydroyura.agents.piflow.workflow.infrastructure.ChangeArtifactFingerprint;
import com.hydroyura.agents.piflow.workflow.infrastructure.CommandVerifier;
import com.hydroyura.agents.piflow.workflow.infrastructure.GitWorktreeCheck;
import com.hydroyura.agents.piflow.workflow.infrastructure.InMemoryWorkflowRepository;
import com.hydroyura.agents.piflow.workflow.infrastructure.OpenSpecChangeResolver;
import com.hydroyura.agents.piflow.workflow.infrastructure.VerificationProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkflowEngineImplTest {

    private static final String CHANGE_ID = "my-change";

    @TempDir
    Path tempDir;

    private Path targetRepo;
    private final InMemoryWorkflowRepository repository = new InMemoryWorkflowRepository();

    private final ChangeResolver resolverOk = (repo, changeId) -> ChangeResolution.resolved(changeId);
    private final ChangeValidator validatorOk = (repo, changeId) -> ValidationResult.ok();
    private final WorktreeCheck worktreeCheck = new GitWorktreeCheck();
    private final ArtifactFingerprint fingerprint = new ChangeArtifactFingerprint();
    private final StageRunner alwaysPass = passWithProposal();
    private final Verifier alwaysVerify = workflow -> VerificationResult.passed("verification passed", false);

    @BeforeEach
    void setUp() throws Exception {
        targetRepo = initRepoWithChange();
    }

    private Path initRepoWithChange() throws Exception {
        Path repo = Files.createDirectories(tempDir.resolve("repo"));
        git(repo, "init");
        createChange(repo, CHANGE_ID);
        return repo;
    }

    private static void createChange(Path repo, String changeId) throws IOException {
        Path changeDir = Files.createDirectories(repo.resolve("openspec/changes/" + changeId));
        Files.writeString(changeDir.resolve("proposal.md"), "# Proposal\n\nWhy: test change.\n");
        Files.writeString(changeDir.resolve("design.md"), "# Design\n\nContext: test change.\n");
        Files.writeString(changeDir.resolve("tasks.md"), "- [ ] 1.1 Do a thing\n");
        Path specs = Files.createDirectories(changeDir.resolve("specs/cap"));
        Files.writeString(specs.resolve("spec.md"),
                "# Spec Delta\n\n## ADDED Requirements\n\n### Requirement: Thing\nBody\n\n#### Scenario: S\n- **WHEN** x\n- **THEN** y\n");
    }

    private Path changeDir() {
        return targetRepo.resolve("openspec/changes/" + CHANGE_ID);
    }

    private static StageRunner passWithProposal() {
        return (stage, workflow, context) -> stage == WorkflowStage.REVIEW
                ? StageOutcome.reviewed(stage, "ok", ReviewVerdict.APPROVED, List.of())
                : stage == WorkflowStage.CHANGE_PROPOSAL
                        ? StageOutcome.changeProposed(stage, "ok", CHANGE_ID)
                        : StageOutcome.success(stage, "ok");
    }

    private WorkflowEngineImpl engine(StageRunner runner, Verifier verifier) {
        return new WorkflowEngineImpl(repository, runner, verifier, worktreeCheck, resolverOk, validatorOk, fingerprint);
    }

    private WorkflowEngineImpl engine(StageRunner runner, Verifier verifier,
                                      ChangeResolver resolver, ChangeValidator validator) {
        return new WorkflowEngineImpl(repository, runner, verifier, worktreeCheck, resolver, validator, fingerprint);
    }

    private Workflow loaded(WorkflowId id) {
        return repository.findById(id).orElseThrow();
    }

    private static long implementationCount(WorkflowView view) {
        return view.outcomes().stream().filter(outcome -> outcome.stage() == WorkflowStage.IMPLEMENTATION).count();
    }

    // --- 1. Canonical-worktree-root preflight ---

    @Test
    void start_withNonGitTargetRepo_shouldRejectBeforeCreatingWorkflow() throws IOException {
        AtomicInteger stageRuns = new AtomicInteger();
        StageRunner counting = (stage, workflow, context) -> {
            stageRuns.incrementAndGet();
            return StageOutcome.success(stage, "ok");
        };
        Path nonGit = Files.createDirectories(tempDir.resolve("not-git"));
        WorkflowEngineImpl engine = engine(counting, alwaysVerify);

        assertThatThrownBy(() -> engine.start("task", nonGit.toString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a Git worktree");

        assertThat(repository.findAll()).isEmpty();
        assertThat(stageRuns.get()).isZero();
    }

    @Test
    void start_withNonExistentTargetRepo_shouldReject() {
        AtomicInteger stageRuns = new AtomicInteger();
        StageRunner counting = (stage, workflow, context) -> {
            stageRuns.incrementAndGet();
            return StageOutcome.success(stage, "ok");
        };
        WorkflowEngineImpl engine = engine(counting, alwaysVerify);

        assertThatThrownBy(() -> engine.start("task", tempDir.resolve("does-not-exist").toString()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(repository.findAll()).isEmpty();
        assertThat(stageRuns.get()).isZero();
    }

    @Test
    void start_withDirtyWorktree_shouldLeaveWorktreeUnmodified() throws Exception {
        Path repo = initRepoWithChange();
        Path tracked = repo.resolve("tracked.txt");
        Files.writeString(tracked, "original");
        git(repo, "add", ".");
        git(repo, "-c", "user.email=t@example.com", "-c", "user.name=t", "commit", "-m", "init");
        Files.writeString(tracked, "modified-uncommitted");
        String statusBefore = gitStatusPorcelain(repo);

        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        engine.start("task", repo.toString());

        assertThat(Files.readString(tracked)).isEqualTo("modified-uncommitted");
        assertThat(gitStatusPorcelain(repo)).isEqualTo(statusBefore);
    }

    // --- existing happy-path / gate / verification tests, updated for the real repo ---

    @Test
    void start_shouldAdvanceToProposalGate() {
        WorkflowView view = engine(alwaysPass, alwaysVerify).start("task", targetRepo.toString());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VALIDATION_APPROVAL);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
    }

    @Test
    void approveThenAccept_shouldComplete() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());

        view = engine.approve(view.id());
        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);

        view = engine.approve(view.id());
        assertThat(view.currentStage()).isEqualTo(WorkflowStage.COMPLETED);
        assertThat(view.status()).isEqualTo(WorkflowStatus.COMPLETED);
    }

    @Test
    void reject_shouldCancel() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());

        view = engine.reject(view.id());

        assertThat(view.status()).isEqualTo(WorkflowStatus.CANCELLED);
    }

    @Test
    void requestRevisions_shouldReturnToProposalAndGate() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());

        view = engine.requestRevisions(view.id(), "");

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VALIDATION_APPROVAL);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
    }

    @Test
    void verification_passed_shouldAdvanceToReview() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());

        view = engine.approve(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
        assertThat(implementationCount(view)).isEqualTo(1);
        assertThat(view.outcomes()).anySatisfy(outcome ->
                assertThat(outcome.stage()).isEqualTo(WorkflowStage.REVIEW));
    }

    @Test
    void verification_failed_shouldReturnToImplementation() {
        AtomicInteger verifyCalls = new AtomicInteger();
        Verifier flaky = workflow -> verifyCalls.getAndIncrement() == 0
                ? VerificationResult.failed("verification failed", false)
                : VerificationResult.passed("verification passed", false);
        WorkflowEngineImpl engine = engine(alwaysPass, flaky);
        WorkflowView view = engine.start("task", targetRepo.toString());

        view = engine.approve(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
        assertThat(implementationCount(view)).isEqualTo(2);
    }

    @Test
    void verification_notConfigured_shouldPauseAtVerificationStage() {
        WorkflowEngineImpl engine = engine(alwaysPass, workflow -> VerificationResult.notConfigured("no config file"));
        WorkflowView view = engine.start("task", targetRepo.toString());

        view = engine.approve(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VERIFICATION);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
        StageOutcome last = view.outcomes().getLast();
        assertThat(last.verificationStatus()).contains(VerificationStatus.NOT_CONFIGURED);
        assertThat(last.output()).contains("no config file");
    }

    @Test
    void verification_infrastructureError_shouldPauseAtVerificationStage() {
        WorkflowEngineImpl engine = engine(alwaysPass,
                workflow -> VerificationResult.infrastructureError("missing executable"));
        WorkflowView view = engine.start("task", targetRepo.toString());

        view = engine.approve(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VERIFICATION);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
        StageOutcome last = view.outcomes().getLast();
        assertThat(last.verificationStatus()).contains(VerificationStatus.INFRASTRUCTURE_ERROR);
        assertThat(last.output()).contains("missing executable");
    }

    @Test
    void verification_pausedStates_shouldNotBeTerminal() {
        WorkflowEngineImpl notConfigured = engine(alwaysPass, workflow -> VerificationResult.notConfigured("no config"));
        WorkflowView view = notConfigured.start("task", targetRepo.toString());
        view = notConfigured.approve(view.id());
        assertThat(view.status()).isNotIn(WorkflowStatus.COMPLETED, WorkflowStatus.CANCELLED, WorkflowStatus.FAILED);

        WorkflowEngineImpl infraError = engine(alwaysPass,
                workflow -> VerificationResult.infrastructureError("broken config"));
        view = infraError.start("task", targetRepo.toString());
        view = infraError.approve(view.id());
        assertThat(view.status()).isNotIn(WorkflowStatus.COMPLETED, WorkflowStatus.CANCELLED, WorkflowStatus.FAILED);
    }

    @Test
    void verification_infrastructureError_shouldNotBeRoutedAsFailed() {
        WorkflowEngineImpl engine = engine(alwaysPass,
                workflow -> VerificationResult.infrastructureError("missing executable"));
        WorkflowView view = engine.start("task", targetRepo.toString());

        view = engine.approve(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VERIFICATION);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
        StageOutcome last = view.outcomes().getLast();
        assertThat(last.verificationStatus()).contains(VerificationStatus.INFRASTRUCTURE_ERROR);
        assertThat(last.output()).contains("missing executable");
    }

    @Test
    void retryVerification_afterNotConfigured_shouldReEvaluateAndRoute() {
        AtomicInteger verifyCalls = new AtomicInteger();
        Verifier stub = workflow -> verifyCalls.getAndIncrement() == 0
                ? VerificationResult.notConfigured("no config file")
                : VerificationResult.passed("verification passed", false);
        WorkflowEngineImpl engine = engine(alwaysPass, stub);
        WorkflowView view = engine.start("task", targetRepo.toString());
        view = engine.approve(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VERIFICATION);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);

        view = engine.retryVerification(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
        assertThat(view.outcomes()).anySatisfy(outcome ->
                assertThat(outcome.stage()).isEqualTo(WorkflowStage.REVIEW));
    }

    @Test
    void retryVerification_afterInfrastructureError_shouldReEvaluateAndRoute() {
        AtomicInteger verifyCalls = new AtomicInteger();
        Verifier stub = workflow -> verifyCalls.getAndIncrement() == 0
                ? VerificationResult.infrastructureError("broken config")
                : VerificationResult.passed("verification passed", false);
        WorkflowEngineImpl engine = engine(alwaysPass, stub);
        WorkflowView view = engine.start("task", targetRepo.toString());
        view = engine.approve(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VERIFICATION);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);

        view = engine.retryVerification(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
    }

    @Test
    void retryVerification_whenNotAtVerificationGate_shouldBeRejected() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");
        workflow.moveTo(WorkflowStage.VALIDATION_APPROVAL);
        workflow.markAwaitingUser();
        repository.save(workflow);

        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);

        assertThatThrownBy(() -> engine.retryVerification(workflow.getId()))
                .isInstanceOf(IllegalStateException.class);

        WorkflowView after = engine.status(workflow.getId());
        assertThat(after.currentStage()).isEqualTo(WorkflowStage.VALIDATION_APPROVAL);
        assertThat(after.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
    }

    @Test
    void retryVerification_endToEnd_withRealCommandVerifier() throws IOException {
        WorkflowEngineImpl engine = engine(alwaysPass, new CommandVerifier(new VerificationProperties(null)));
        WorkflowView view = engine.start("task", targetRepo.toString());
        view = engine.approve(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VERIFICATION);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);

        Path configDir = Files.createDirectories(targetRepo.resolve(".pi-flow"));
        Files.writeString(configDir.resolve("verify.json"),
                "{\"command\":[\"sh\",\"-c\",\"exit 0\"],\"timeoutSeconds\":10}");

        view = engine.retryVerification(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
    }

    @Test
    void status_shouldLoadPersistedState() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView started = engine.start("task", targetRepo.toString());

        WorkflowView loaded = engine.status(started.id());

        assertThat(loaded.currentStage()).isEqualTo(started.currentStage());
        assertThat(loaded.status()).isEqualTo(started.status());
    }

    @Test
    void reject_thenApprove_shouldNotResurrectCancelledWorkflow() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());
        engine.reject(view.id());

        assertThatThrownBy(() -> engine.approve(view.id()))
                .isInstanceOf(IllegalStateException.class);

        WorkflowView after = engine.status(view.id());
        assertThat(after.status()).isEqualTo(WorkflowStatus.CANCELLED);
        assertThat(after.currentStage()).isEqualTo(WorkflowStage.VALIDATION_APPROVAL);
    }

    @Test
    void approve_whileInProgress_shouldBeRejected() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");
        workflow.moveTo(WorkflowStage.VALIDATION_APPROVAL);
        workflow.markInProgress();
        repository.save(workflow);

        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);

        assertThatThrownBy(() -> engine.approve(workflow.getId()))
                .isInstanceOf(IllegalStateException.class);

        WorkflowView after = engine.status(workflow.getId());
        assertThat(after.currentStage()).isEqualTo(WorkflowStage.VALIDATION_APPROVAL);
        assertThat(after.status()).isEqualTo(WorkflowStatus.IN_PROGRESS);
    }

    @Test
    void approve_onCompletedWorkflow_shouldBeRejected() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView started = engine.start("task", targetRepo.toString());
        WorkflowView accepted = engine.approve(started.id());
        WorkflowView completed = engine.approve(accepted.id());

        assertThat(completed.status()).isEqualTo(WorkflowStatus.COMPLETED);

        assertThatThrownBy(() -> engine.approve(completed.id()))
                .isInstanceOf(IllegalStateException.class);

        WorkflowView after = engine.status(completed.id());
        assertThat(after.status()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(after.currentStage()).isEqualTo(WorkflowStage.COMPLETED);
    }

    @Test
    void reject_onCancelledWorkflow_shouldBeRejected() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView started = engine.start("task", targetRepo.toString());
        WorkflowView cancelled = engine.reject(started.id());

        assertThatThrownBy(() -> engine.reject(cancelled.id()))
                .isInstanceOf(IllegalStateException.class);

        WorkflowView after = engine.status(cancelled.id());
        assertThat(after.status()).isEqualTo(WorkflowStatus.CANCELLED);
        assertThat(after.currentStage()).isEqualTo(WorkflowStage.VALIDATION_APPROVAL);
    }

    @Test
    void requestFurtherWork_onFailedWorkflow_shouldBeRejected() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");
        workflow.moveTo(WorkflowStage.ACCEPTANCE);
        workflow.markFailed();
        repository.save(workflow);

        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);

        assertThatThrownBy(() -> engine.requestFurtherWork(workflow.getId(), ""))
                .isInstanceOf(IllegalStateException.class);

        WorkflowView after = engine.status(workflow.getId());
        assertThat(after.status()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(after.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);
    }

    @Test
    void stageRunnerException_shouldRecordFailureAndMarkFailed() {
        StageRunner throwing = (stage, workflow, context) -> {
            if (stage == WorkflowStage.IMPLEMENTATION) {
                throw new RuntimeException("boom");
            }
            if (stage == WorkflowStage.CHANGE_PROPOSAL) {
                return StageOutcome.changeProposed(stage, "ok", CHANGE_ID);
            }
            return StageOutcome.success(stage, "ok");
        };
        WorkflowEngineImpl engine = engine(throwing, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());

        view = engine.approve(view.id());

        assertThat(view.status()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(view.outcomes()).anySatisfy(outcome -> {
            assertThat(outcome.stage()).isEqualTo(WorkflowStage.IMPLEMENTATION);
            assertThat(outcome.success()).isFalse();
            assertThat(outcome.output()).contains("RuntimeException").contains("boom");
        });
    }

    @Test
    void verifierException_shouldRecordFailureAndMarkFailed() {
        Verifier throwing = workflow -> {
            throw new RuntimeException("verify boom");
        };
        WorkflowEngineImpl engine = engine(alwaysPass, throwing);
        WorkflowView view = engine.start("task", targetRepo.toString());

        view = engine.approve(view.id());

        assertThat(view.status()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(view.outcomes()).anySatisfy(outcome -> {
            assertThat(outcome.stage()).isEqualTo(WorkflowStage.VERIFICATION);
            assertThat(outcome.success()).isFalse();
            assertThat(outcome.output()).contains("RuntimeException").contains("verify boom");
        });
    }

    @Test
    void stageRunnerError_shouldPropagateUncaught() {
        StageRunner errorThrowing = (stage, workflow, context) -> {
            if (stage == WorkflowStage.PROJECT_ANALYSIS) {
                throw new AssertionError("fatal");
            }
            return StageOutcome.success(stage, "ok");
        };
        WorkflowEngineImpl engine = engine(errorThrowing, alwaysVerify);

        assertThatThrownBy(() -> engine.start("task", targetRepo.toString()))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void advance_shouldThreadPriorOutcomesIntoStageContext() {
        List<StageContext> contexts = new ArrayList<>();
        StageRunner recording = (stage, workflow, context) -> {
            contexts.add(context);
            return switch (stage) {
                case PROJECT_ANALYSIS -> StageOutcome.success(stage, "analysis-output");
                case CHANGE_PROPOSAL -> StageOutcome.changeProposed(stage, "proposal-output", CHANGE_ID);
                case REVIEW -> StageOutcome.reviewed(stage, "reviewed", ReviewVerdict.APPROVED, List.of());
                default -> StageOutcome.success(stage, "ok");
            };
        };
        WorkflowEngineImpl engine = engine(recording, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());
        view = engine.approve(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);
        assertThat(contexts).hasSize(4);
        assertThat(contexts.get(1).analysisFindings()).contains("analysis-output");
        assertThat(contexts.get(1).proposalOutput()).isEmpty();
        assertThat(contexts.get(2).analysisFindings()).contains("analysis-output");
        assertThat(contexts.get(2).proposalOutput()).contains("proposal-output");
        assertThat(contexts.get(3).analysisFindings()).contains("analysis-output");
        assertThat(contexts.get(3).proposalOutput()).contains("proposal-output");
    }

    @Test
    void projectAnalysisStageContext_shouldHaveNoPriorFindings() {
        List<StageContext> contexts = new ArrayList<>();
        StageRunner recording = (stage, workflow, context) -> {
            contexts.add(context);
            return switch (stage) {
                case CHANGE_PROPOSAL -> StageOutcome.changeProposed(stage, "ok", CHANGE_ID);
                case REVIEW -> StageOutcome.reviewed(stage, "ok", ReviewVerdict.APPROVED, List.of());
                default -> StageOutcome.success(stage, "ok");
            };
        };
        engine(recording, alwaysVerify).start("task", targetRepo.toString());

        assertThat(contexts).isNotEmpty();
        StageContext first = contexts.getFirst();
        assertThat(first.analysisFindings()).isEmpty();
        assertThat(first.proposalOutput()).isEmpty();
    }

    @Test
    void consumedFeedback_shouldNotLeakIntoLaterStages() {
        List<StageContext> contexts = new ArrayList<>();
        StageRunner recording = (stage, workflow, context) -> {
            contexts.add(context);
            return switch (stage) {
                case CHANGE_PROPOSAL -> StageOutcome.changeProposed(stage, "ok", CHANGE_ID);
                case REVIEW -> StageOutcome.reviewed(stage, "ok", ReviewVerdict.APPROVED, List.of());
                default -> StageOutcome.success(stage, "ok");
            };
        };
        WorkflowEngineImpl engine = engine(recording, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());
        view = engine.requestRevisions(view.id(), "fix this");
        view = engine.approve(view.id());

        assertThat(contexts).hasSize(5);
        assertThat(contexts.get(2).pendingFeedback()).contains("fix this");
        assertThat(contexts.get(3).pendingFeedback()).isEmpty();
        assertThat(contexts.get(4).pendingFeedback()).isEmpty();
    }

    @Test
    void requestRevisions_withFeedback_shouldThreadFeedbackIntoNextProposalStage() {
        List<StageContext> contexts = new ArrayList<>();
        StageRunner recording = (stage, workflow, context) -> {
            contexts.add(context);
            return switch (stage) {
                case CHANGE_PROPOSAL -> StageOutcome.changeProposed(stage, "ok", CHANGE_ID);
                case REVIEW -> StageOutcome.reviewed(stage, "ok", ReviewVerdict.APPROVED, List.of());
                default -> StageOutcome.success(stage, "ok");
            };
        };
        WorkflowEngineImpl engine = engine(recording, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());
        view = engine.requestRevisions(view.id(), "the error handling section is missing");

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VALIDATION_APPROVAL);
        assertThat(contexts.getLast().pendingFeedback()).contains("the error handling section is missing");
    }

    @Test
    void requestFurtherWork_withFeedback_shouldThreadFeedbackIntoNextImplementationStage() {
        List<StageContext> contexts = new ArrayList<>();
        StageRunner recording = (stage, workflow, context) -> {
            contexts.add(context);
            return switch (stage) {
                case CHANGE_PROPOSAL -> StageOutcome.changeProposed(stage, "ok", CHANGE_ID);
                case REVIEW -> StageOutcome.reviewed(stage, "ok", ReviewVerdict.APPROVED, List.of());
                default -> StageOutcome.success(stage, "ok");
            };
        };
        WorkflowEngineImpl engine = engine(recording, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());
        view = engine.approve(view.id());
        view = engine.requestFurtherWork(view.id(), "please add a test for the empty-input case");

        assertThat(contexts).hasSize(6);
        assertThat(contexts.get(4).pendingFeedback()).contains("please add a test for the empty-input case");
        assertThat(contexts.get(5).pendingFeedback()).isEmpty();
    }

    @Test
    void requestRevisions_withEmptyFeedback_shouldSucceed() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());

        view = engine.requestRevisions(view.id(), "");

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VALIDATION_APPROVAL);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
    }

    @Test
    void review_approved_shouldAdvanceToAcceptance() {
        StageRunner runner = (stage, workflow, context) ->
                stage == WorkflowStage.REVIEW
                        ? StageOutcome.reviewed(stage, "ok", ReviewVerdict.APPROVED, List.of())
                        : stage == WorkflowStage.CHANGE_PROPOSAL
                                ? StageOutcome.changeProposed(stage, "ok", CHANGE_ID)
                                : StageOutcome.success(stage, "ok");
        WorkflowEngineImpl engine = engine(runner, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());

        view = engine.approve(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
    }

    @Test
    void review_changesRequired_shouldReturnToImplementation() {
        AtomicInteger reviewCalls = new AtomicInteger();
        AtomicInteger implementationCalls = new AtomicInteger();
        StageRunner runner = (stage, workflow, context) -> {
            if (stage == WorkflowStage.IMPLEMENTATION) {
                implementationCalls.incrementAndGet();
            }
            if (stage == WorkflowStage.REVIEW && reviewCalls.getAndIncrement() == 0) {
                return StageOutcome.reviewed(stage, "needs corrections", ReviewVerdict.CHANGES_REQUIRED, List.of("fix x"));
            }
            if (stage == WorkflowStage.CHANGE_PROPOSAL) {
                return StageOutcome.changeProposed(stage, "ok", CHANGE_ID);
            }
            return stage == WorkflowStage.REVIEW
                    ? StageOutcome.reviewed(stage, "ok", ReviewVerdict.APPROVED, List.of())
                    : StageOutcome.success(stage, "ok");
        };
        WorkflowEngineImpl engine = engine(runner, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());

        view = engine.approve(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
        assertThat(implementationCalls.get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void review_technicalFailure_shouldMarkFailed_notLoop() {
        StageRunner runner = (stage, workflow, context) ->
                stage == WorkflowStage.REVIEW
                        ? StageOutcome.failure(stage, "reviewer crashed")
                        : stage == WorkflowStage.CHANGE_PROPOSAL
                                ? StageOutcome.changeProposed(stage, "ok", CHANGE_ID)
                                : StageOutcome.success(stage, "ok");
        WorkflowEngineImpl engine = engine(runner, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());

        view = engine.approve(view.id());

        assertThat(view.status()).isEqualTo(WorkflowStatus.FAILED);
    }

    @Test
    void review_malformedVerdict_shouldMarkFailed() {
        StageRunner runner = (stage, workflow, context) ->
                stage == WorkflowStage.REVIEW
                        ? StageOutcome.success(stage, "ok")
                        : stage == WorkflowStage.CHANGE_PROPOSAL
                                ? StageOutcome.changeProposed(stage, "ok", CHANGE_ID)
                                : StageOutcome.success(stage, "ok");
        WorkflowEngineImpl engine = engine(runner, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());

        view = engine.approve(view.id());

        assertThat(view.status()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(view.currentStage()).isNotEqualTo(WorkflowStage.ACCEPTANCE);
    }

    // --- 4. Binding and validation after CHANGE_PROPOSAL ---

    @Test
    void changeProposal_withMissingIdentifier_shouldFailStageAndNotPresentGate() {
        StageRunner missingId = (stage, workflow, context) ->
                stage == WorkflowStage.CHANGE_PROPOSAL
                        ? StageOutcome.success(stage, "ok")
                        : StageOutcome.success(stage, "ok");
        WorkflowEngineImpl engine = engine(missingId, alwaysVerify);

        WorkflowView view = engine.start("task", targetRepo.toString());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.CHANGE_PROPOSAL);
        assertThat(view.status()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(implementationCount(view)).isZero();
    }

    @Test
    void changeProposal_withAmbiguousIdentifier_shouldFailStageAndNotPresentGate() {
        ChangeResolver ambiguous = (repo, changeId) -> ChangeResolution.ambiguous("more than one match");
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify, ambiguous, validatorOk);

        WorkflowView view = engine.start("task", targetRepo.toString());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.CHANGE_PROPOSAL);
        assertThat(view.status()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(implementationCount(view)).isZero();
    }

    @Test
    void changeProposal_withPathEscapingIdentifier_shouldFailStageAndNotPresentGate() {
        ChangeResolver pathEscaping = new OpenSpecChangeResolver(
                tempDir.resolve("no-such-openspec").toString(), Duration.ofSeconds(5));
        StageRunner runner = (stage, workflow, context) ->
                stage == WorkflowStage.CHANGE_PROPOSAL
                        ? StageOutcome.changeProposed(stage, "ok", "../../etc")
                        : StageOutcome.success(stage, "ok");
        WorkflowEngineImpl engine = engine(runner, alwaysVerify, pathEscaping, validatorOk);

        WorkflowView view = engine.start("task", targetRepo.toString());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.CHANGE_PROPOSAL);
        assertThat(view.status()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(implementationCount(view)).isZero();
    }

    @Test
    void changeProposal_withFailingValidation_shouldFailStageAndNotPresentGate() {
        ChangeValidator failing = (repo, changeId) -> ValidationResult.failed("spec is invalid");
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify, resolverOk, failing);

        WorkflowView view = engine.start("task", targetRepo.toString());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.CHANGE_PROPOSAL);
        assertThat(view.status()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(implementationCount(view)).isZero();
    }

    @Test
    void changeProposal_withAllChecksPassing_shouldBindAndPresentGate() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);

        WorkflowView view = engine.start("task", targetRepo.toString());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VALIDATION_APPROVAL);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
        assertThat(loaded(view.id()).getChangeId()).contains(CHANGE_ID);
    }

    @Test
    void changeProposal_revision_shouldStayBoundToSameChange() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());

        view = engine.requestRevisions(view.id(), "revise");

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VALIDATION_APPROVAL);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
        assertThat(loaded(view.id()).getChangeId()).contains(CHANGE_ID);
    }

    @Test
    void changeProposal_revisionNamingDifferentChange_shouldFailStage() {
        AtomicInteger proposalCalls = new AtomicInteger();
        StageRunner runner = (stage, workflow, context) -> {
            if (stage == WorkflowStage.CHANGE_PROPOSAL) {
                return proposalCalls.getAndIncrement() == 0
                        ? StageOutcome.changeProposed(stage, "ok", CHANGE_ID)
                        : StageOutcome.changeProposed(stage, "ok", "other-change");
            }
            return StageOutcome.success(stage, "ok");
        };
        WorkflowEngineImpl engine = engine(runner, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());
        assertThat(loaded(view.id()).getChangeId()).contains(CHANGE_ID);

        view = engine.requestRevisions(view.id(), "revise");

        assertThat(view.status()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(view.currentStage()).isEqualTo(WorkflowStage.CHANGE_PROPOSAL);
        assertThat(loaded(view.id()).getChangeId()).contains(CHANGE_ID);
        assertThat(view.outcomes().getLast().success()).isFalse();
    }

    @Test
    void bindingValidation_shouldLeaveDirtyWorktreeUnmodified() throws Exception {
        Path repo = initRepoWithChange();
        Path tracked = repo.resolve("tracked.txt");
        Files.writeString(tracked, "original");
        git(repo, "add", ".");
        git(repo, "-c", "user.email=t@example.com", "-c", "user.name=t", "commit", "-m", "init");
        Files.writeString(tracked, "modified-uncommitted");
        String statusBefore = gitStatusPorcelain(repo);

        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", repo.toString());
        assertThat(loaded(view.id()).getChangeId()).contains(CHANGE_ID);

        assertThat(Files.readString(tracked)).isEqualTo("modified-uncommitted");
        assertThat(gitStatusPorcelain(repo)).isEqualTo(statusBefore);
    }

    // --- 5. Approval fingerprint ---

    @Test
    void gateOpen_shouldRecordFingerprintOverCurrentArtifacts() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());

        Workflow workflow = loaded(view.id());
        String expected = fingerprint.compute(changeDir());

        assertThat(workflow.getApprovalFingerprint()).contains(expected);
    }

    @Test
    void approve_withUnchangedArtifacts_shouldRecordApprovalAndProceed() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());
        String gateFingerprint = loaded(view.id()).getApprovalFingerprint().orElseThrow();

        view = engine.approve(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);
        assertThat(loaded(view.id()).getApprovalFingerprint()).contains(gateFingerprint);
        assertThat(implementationCount(view)).isEqualTo(1);
    }

    @Test
    void approve_withDriftedArtifacts_shouldBlockApprovalAndReopenGateWithFreshFingerprint() throws IOException {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());
        String stale = loaded(view.id()).getApprovalFingerprint().orElseThrow();

        Files.writeString(changeDir().resolve("proposal.md"), "# Proposal\n\nWhy: drifted\n");

        view = engine.approve(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VALIDATION_APPROVAL);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
        assertThat(loaded(view.id()).getApprovalFingerprint()).contains(fingerprint.compute(changeDir()));
        assertThat(loaded(view.id()).getApprovalFingerprint().orElseThrow()).isNotEqualTo(stale);
    }

    @Test
    void implementation_withUnchangedArtifacts_shouldProceed() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());
        view = engine.approve(view.id());
        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);

        view = engine.requestFurtherWork(view.id(), "further");

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);
        assertThat(implementationCount(view)).isEqualTo(2);
    }

    @Test
    void implementation_withDriftedArtifacts_shouldBlockReopenGateAndRevalidate() throws IOException {
        AtomicInteger implementationRuns = new AtomicInteger();
        StageRunner runner = (stage, workflow, context) -> {
            if (stage == WorkflowStage.IMPLEMENTATION) {
                implementationRuns.incrementAndGet();
            }
            return stage == WorkflowStage.REVIEW
                    ? StageOutcome.reviewed(stage, "ok", ReviewVerdict.APPROVED, List.of())
                    : stage == WorkflowStage.CHANGE_PROPOSAL
                            ? StageOutcome.changeProposed(stage, "ok", CHANGE_ID)
                            : StageOutcome.success(stage, "ok");
        };
        WorkflowEngineImpl engine = engine(runner, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());
        view = engine.approve(view.id());
        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);
        assertThat(implementationRuns.get()).isEqualTo(1);
        String stale = loaded(view.id()).getApprovalFingerprint().orElseThrow();

        Files.writeString(changeDir().resolve("design.md"), "# Design\n\nContext: drifted\n");

        view = engine.requestFurtherWork(view.id(), "further");

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VALIDATION_APPROVAL);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
        assertThat(implementationRuns.get()).isEqualTo(1);
        assertThat(loaded(view.id()).getApprovalFingerprint().orElseThrow()).isNotEqualTo(stale);
        assertThat(view.outcomes()).anySatisfy(outcome ->
                assertThat(outcome.output()).contains("drift"));
    }

    @Test
    void tasksMdCheckboxToggle_shouldNotTriggerDriftAtApproval() throws IOException {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());

        Files.writeString(changeDir().resolve("tasks.md"), "- [x] 1.1 Do a thing\n");

        view = engine.approve(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
        assertThat(implementationCount(view)).isEqualTo(1);
    }

    @Test
    void tasksMdCheckboxToggle_shouldNotTriggerDriftBeforeImplementation() throws IOException {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());
        view = engine.approve(view.id());
        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);

        Files.writeString(changeDir().resolve("tasks.md"), "- [x] 1.1 Do a thing\n");

        view = engine.requestFurtherWork(view.id(), "further");

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);
        assertThat(implementationCount(view)).isEqualTo(2);
    }

    @Test
    void approve_afterDriftReopened_shouldRecordNewFingerprintAndProceedOnReapproval() throws IOException {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", targetRepo.toString());

        Files.writeString(changeDir().resolve("proposal.md"), "# Proposal\n\nWhy: drifted\n");
        view = engine.approve(view.id());
        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VALIDATION_APPROVAL);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
        String fresh = loaded(view.id()).getApprovalFingerprint().orElseThrow();

        view = engine.approve(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
        assertThat(implementationCount(view)).isEqualTo(1);
        assertThat(loaded(view.id()).getApprovalFingerprint()).contains(fresh);
    }

    // --- 6. No Git writes anywhere in this change's checks ---

    @Test
    void fullRun_shouldLeaveDirtyWorktreeUnmodifiedAtEveryCheckpoint() throws Exception {
        Path repo = initRepoWithChange();
        Path tracked = repo.resolve("tracked.txt");
        Files.writeString(tracked, "original");
        git(repo, "add", ".");
        git(repo, "-c", "user.email=t@example.com", "-c", "user.name=t", "commit", "-m", "init");
        Files.writeString(tracked, "modified-uncommitted");
        String statusBefore = gitStatusPorcelain(repo);

        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", repo.toString());
        assertThat(Files.readString(tracked)).isEqualTo("modified-uncommitted");
        assertThat(gitStatusPorcelain(repo)).isEqualTo(statusBefore);

        view = engine.approve(view.id());
        assertThat(Files.readString(tracked)).isEqualTo("modified-uncommitted");
        assertThat(gitStatusPorcelain(repo)).isEqualTo(statusBefore);
    }

    // --- helpers ---

    private static void git(Path dir, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(dir.toFile());
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("git " + String.join(" ", args) + " failed (" + exit + "): " + output);
        }
    }

    private static String gitStatusPorcelain(Path dir) throws Exception {
        List<String> command = List.of("git", "-C", dir.toString(), "status", "--porcelain");
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("git status failed (" + exit + "): " + output);
        }
        return output;
    }
}
