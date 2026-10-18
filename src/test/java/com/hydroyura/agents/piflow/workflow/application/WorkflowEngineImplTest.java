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
import com.hydroyura.agents.piflow.workflow.infrastructure.CommandVerifier;
import com.hydroyura.agents.piflow.workflow.infrastructure.InMemoryWorkflowRepository;
import com.hydroyura.agents.piflow.workflow.infrastructure.VerificationProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkflowEngineImplTest {

    @TempDir
    Path tempDir;

    private final InMemoryWorkflowRepository repository = new InMemoryWorkflowRepository();
    private final StageRunner alwaysPass = (stage, workflow, context) ->
            stage == WorkflowStage.REVIEW
                    ? StageOutcome.reviewed(stage, "ok", ReviewVerdict.APPROVED, List.of())
                    : StageOutcome.success(stage, "ok");
    private final Verifier alwaysVerify = workflow -> VerificationResult.passed("verification passed", false);

    private WorkflowEngineImpl engine(StageRunner runner, Verifier verifier) {
        return new WorkflowEngineImpl(repository, runner, verifier);
    }

    private static long implementationCount(WorkflowView view) {
        return view.outcomes().stream().filter(outcome -> outcome.stage() == WorkflowStage.IMPLEMENTATION).count();
    }

    @Test
    void start_shouldAdvanceToProposalGate() {
        WorkflowView view = engine(alwaysPass, alwaysVerify).start("task", "/repo");

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VALIDATION_APPROVAL);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
    }

    @Test
    void approveThenAccept_shouldComplete() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", "/repo");

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
        WorkflowView view = engine.start("task", "/repo");

        view = engine.reject(view.id());

        assertThat(view.status()).isEqualTo(WorkflowStatus.CANCELLED);
    }

    @Test
    void requestRevisions_shouldReturnToProposalAndGate() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", "/repo");

        view = engine.requestRevisions(view.id(), "");

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VALIDATION_APPROVAL);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
    }

    @Test
    void verification_passed_shouldAdvanceToReview() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", "/repo");

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
        WorkflowView view = engine.start("task", "/repo");

        view = engine.approve(view.id());

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.ACCEPTANCE);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
        assertThat(implementationCount(view)).isEqualTo(2);
    }

    @Test
    void verification_notConfigured_shouldPauseAtVerificationStage() {
        WorkflowEngineImpl engine = engine(alwaysPass, workflow -> VerificationResult.notConfigured("no config file"));
        WorkflowView view = engine.start("task", "/repo");

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
        WorkflowView view = engine.start("task", "/repo");

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
        WorkflowView view = notConfigured.start("task", "/repo");
        view = notConfigured.approve(view.id());
        assertThat(view.status()).isNotIn(WorkflowStatus.COMPLETED, WorkflowStatus.CANCELLED, WorkflowStatus.FAILED);

        WorkflowEngineImpl infraError = engine(alwaysPass,
                workflow -> VerificationResult.infrastructureError("broken config"));
        view = infraError.start("task", "/repo");
        view = infraError.approve(view.id());
        assertThat(view.status()).isNotIn(WorkflowStatus.COMPLETED, WorkflowStatus.CANCELLED, WorkflowStatus.FAILED);
    }

    @Test
    void verification_infrastructureError_shouldNotBeRoutedAsFailed() {
        WorkflowEngineImpl engine = engine(alwaysPass,
                workflow -> VerificationResult.infrastructureError("missing executable"));
        WorkflowView view = engine.start("task", "/repo");

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
        WorkflowView view = engine.start("task", "/repo");
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
        WorkflowView view = engine.start("task", "/repo");
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
        Path targetRepo = Files.createDirectories(tempDir.resolve("e2e-repo"));
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
        WorkflowView started = engine.start("task", "/repo");

        WorkflowView loaded = engine.status(started.id());

        assertThat(loaded.currentStage()).isEqualTo(started.currentStage());
        assertThat(loaded.status()).isEqualTo(started.status());
    }

    @Test
    void reject_thenApprove_shouldNotResurrectCancelledWorkflow() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", "/repo");
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
        WorkflowView started = engine.start("task", "/repo");
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
        WorkflowView started = engine.start("task", "/repo");
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
            return StageOutcome.success(stage, "ok");
        };
        WorkflowEngineImpl engine = engine(throwing, alwaysVerify);
        WorkflowView view = engine.start("task", "/repo");

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
        WorkflowView view = engine.start("task", "/repo");

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

        assertThatThrownBy(() -> engine.start("task", "/repo"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void advance_shouldThreadPriorOutcomesIntoStageContext() {
        List<StageContext> contexts = new ArrayList<>();
        StageRunner recording = (stage, workflow, context) -> {
            contexts.add(context);
            return switch (stage) {
                case PROJECT_ANALYSIS -> StageOutcome.success(stage, "analysis-output");
                case CHANGE_PROPOSAL -> StageOutcome.success(stage, "proposal-output");
                case REVIEW -> StageOutcome.reviewed(stage, "reviewed", ReviewVerdict.APPROVED, List.of());
                default -> StageOutcome.success(stage, "ok");
            };
        };
        WorkflowEngineImpl engine = engine(recording, alwaysVerify);
        WorkflowView view = engine.start("task", "/repo");
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
            return stage == WorkflowStage.REVIEW
                    ? StageOutcome.reviewed(stage, "ok", ReviewVerdict.APPROVED, List.of())
                    : StageOutcome.success(stage, "ok");
        };
        engine(recording, alwaysVerify).start("task", "/repo");

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
            return stage == WorkflowStage.REVIEW
                    ? StageOutcome.reviewed(stage, "ok", ReviewVerdict.APPROVED, List.of())
                    : StageOutcome.success(stage, "ok");
        };
        WorkflowEngineImpl engine = engine(recording, alwaysVerify);
        WorkflowView view = engine.start("task", "/repo");
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
            return stage == WorkflowStage.REVIEW
                    ? StageOutcome.reviewed(stage, "ok", ReviewVerdict.APPROVED, List.of())
                    : StageOutcome.success(stage, "ok");
        };
        WorkflowEngineImpl engine = engine(recording, alwaysVerify);
        WorkflowView view = engine.start("task", "/repo");
        view = engine.requestRevisions(view.id(), "the error handling section is missing");

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VALIDATION_APPROVAL);
        assertThat(contexts.getLast().pendingFeedback()).contains("the error handling section is missing");
    }

    @Test
    void requestFurtherWork_withFeedback_shouldThreadFeedbackIntoNextImplementationStage() {
        List<StageContext> contexts = new ArrayList<>();
        StageRunner recording = (stage, workflow, context) -> {
            contexts.add(context);
            return stage == WorkflowStage.REVIEW
                    ? StageOutcome.reviewed(stage, "ok", ReviewVerdict.APPROVED, List.of())
                    : StageOutcome.success(stage, "ok");
        };
        WorkflowEngineImpl engine = engine(recording, alwaysVerify);
        WorkflowView view = engine.start("task", "/repo");
        view = engine.approve(view.id());
        view = engine.requestFurtherWork(view.id(), "please add a test for the empty-input case");

        assertThat(contexts).hasSize(6);
        assertThat(contexts.get(4).pendingFeedback()).contains("please add a test for the empty-input case");
        assertThat(contexts.get(5).pendingFeedback()).isEmpty();
    }

    @Test
    void requestRevisions_withEmptyFeedback_shouldSucceed() {
        WorkflowEngineImpl engine = engine(alwaysPass, alwaysVerify);
        WorkflowView view = engine.start("task", "/repo");

        view = engine.requestRevisions(view.id(), "");

        assertThat(view.currentStage()).isEqualTo(WorkflowStage.VALIDATION_APPROVAL);
        assertThat(view.status()).isEqualTo(WorkflowStatus.AWAITING_USER);
    }

    @Test
    void review_approved_shouldAdvanceToAcceptance() {
        StageRunner runner = (stage, workflow, context) ->
                stage == WorkflowStage.REVIEW
                        ? StageOutcome.reviewed(stage, "ok", ReviewVerdict.APPROVED, List.of())
                        : StageOutcome.success(stage, "ok");
        WorkflowEngineImpl engine = engine(runner, alwaysVerify);
        WorkflowView view = engine.start("task", "/repo");

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
            return stage == WorkflowStage.REVIEW
                    ? StageOutcome.reviewed(stage, "ok", ReviewVerdict.APPROVED, List.of())
                    : StageOutcome.success(stage, "ok");
        };
        WorkflowEngineImpl engine = engine(runner, alwaysVerify);
        WorkflowView view = engine.start("task", "/repo");

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
                        : StageOutcome.success(stage, "ok");
        WorkflowEngineImpl engine = engine(runner, alwaysVerify);
        WorkflowView view = engine.start("task", "/repo");

        view = engine.approve(view.id());

        assertThat(view.status()).isEqualTo(WorkflowStatus.FAILED);
    }

    @Test
    void review_malformedVerdict_shouldMarkFailed() {
        StageRunner runner = (stage, workflow, context) ->
                stage == WorkflowStage.REVIEW
                        ? StageOutcome.success(stage, "ok")
                        : StageOutcome.success(stage, "ok");
        WorkflowEngineImpl engine = engine(runner, alwaysVerify);
        WorkflowView view = engine.start("task", "/repo");

        view = engine.approve(view.id());

        assertThat(view.status()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(view.currentStage()).isNotEqualTo(WorkflowStage.ACCEPTANCE);
    }
}
