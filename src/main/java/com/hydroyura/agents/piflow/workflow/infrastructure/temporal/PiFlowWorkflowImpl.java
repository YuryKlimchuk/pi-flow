package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * Реализация единого длинного workflow: preparation → Analyst (gate) →
 * Architect (gate) → Developer (rework до 3) → Reviewer (gate) → finalization
 * (branch + commit + push + PR) → release lock.
 *
 * <p>Ручные этапы ждут сигнала continue/rework через {@link Workflow#await};
 * все локальные Git, GitHub и subprocess операции выполняются в activities.</p>
 */
public class PiFlowWorkflowImpl implements PiFlowWorkflow {

	private static final org.slf4j.Logger log = Workflow.getLogger(PiFlowWorkflowImpl.class);

	private static final int MAX_REWORK = 3;

	private static final RetryOptions RETRY_OPTIONS = RetryOptions.newBuilder()
			.setMaximumAttempts(3)
			.setInitialInterval(Duration.ofSeconds(1))
			.setBackoffCoefficient(2.0)
			.setMaximumInterval(Duration.ofSeconds(10))
			.build();

	private static final RetryOptions NO_RETRY = RetryOptions.newBuilder()
			.setMaximumAttempts(1)
			.build();

	private final WorkflowPreparationActivities preparation = Workflow.newActivityStub(
			WorkflowPreparationActivities.class,
			ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(10)).setRetryOptions(RETRY_OPTIONS).build());

	private final WorkflowAnalysisActivities analysis = Workflow.newActivityStub(
			WorkflowAnalysisActivities.class,
			ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(30)).setRetryOptions(RETRY_OPTIONS).build());

	private final WorkflowDesignActivities design = Workflow.newActivityStub(
			WorkflowDesignActivities.class,
			ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(30)).setRetryOptions(RETRY_OPTIONS).build());

	private final WorkflowDeveloperActivities developer = Workflow.newActivityStub(
			WorkflowDeveloperActivities.class,
			ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(60)).setRetryOptions(RETRY_OPTIONS).build());

	private final WorkflowReviewerActivities reviewer = Workflow.newActivityStub(
			WorkflowReviewerActivities.class,
			ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(30)).setRetryOptions(RETRY_OPTIONS).build());

	private final WorkflowFinalizationActivities finalization = Workflow.newActivityStub(
			WorkflowFinalizationActivities.class,
			ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(10)).setRetryOptions(NO_RETRY).build());

	private final WorkflowCleanupActivities cleanup = Workflow.newActivityStub(
			WorkflowCleanupActivities.class,
			ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(5)).setRetryOptions(NO_RETRY).build());

	private String stage = "ANALYZING_REQUIREMENTS";
	private String status = "RUNNING";
	private String workspacePath;
	private String branchName;
	private String implementationRunId;
	private String implementationDiagnostics;
	private String reviewRunId;
	private String reviewText;
	private String commitId;
	private String branchUrl;
	private long prNumber;
	private String prUrl;
	private String failureReason;
	private String lockKey;
	private String lockToken;
	private String baseBranch;
	private String pendingCommand;
	private String pendingFeedback;
	private int implementationReworkCount;
	private int reviewReworkCount;

	@Override
	public void run(WorkflowInput input) {
		try {
			WorkflowPreparationResult prepared = preparation.prepare(input);
			workspacePath = prepared.path();
			lockKey = prepared.lockKey();
			lockToken = prepared.lockToken();
			baseBranch = prepared.baseBranch() != null ? prepared.baseBranch() : "master";
			log.info("workspace prepared: workflowId={} workspace={} baseBranch={}",
					input.workflowId(), workspacePath, baseBranch);

			runAnalystGate(input);
			runArchitectGate(input);
			runImplementationStage(input, null);
			runReviewGate(input);
			runFinalization(input);

			stage = "READY_FOR_PULL_REQUEST";
			status = "COMPLETED";
			log.info("workflow completed: workflowId={} pr={} url={}", input.workflowId(), prNumber, prUrl);
		} catch (RuntimeException e) {
			failureReason = e.getMessage() == null ? "workflow failed" : e.getMessage();
			status = "FAILED";
			log.warn("workflow failed: workflowId={} reason={}", input.workflowId(), failureReason, e);
		} finally {
			releaseLock();
		}
	}

	private void runAnalystGate(WorkflowInput input) {
		stage = "ANALYZING_REQUIREMENTS";
		String feedback = null;
		while (true) {
			status = "RUNNING";
			WorkflowAnalysisResult result = analysis.analyze(new WorkflowAnalysisInput(
					input.workflowId(), input.repositoryUrl(), input.owner(), input.repository(), input.issueNumber(),
					workspacePath, feedback, null));
			workspacePath = result.workspacePath();
			branchName = result.branchName();
			status = "WAITING_FOR_USER";
			feedback = null;
			awaitCommand();
			String command = pendingCommand;
			String f = pendingFeedback;
			pendingCommand = null;
			pendingFeedback = null;
			if ("rework".equals(command)) {
				feedback = f;
				continue;
			}
			break;
		}
	}

	private void runArchitectGate(WorkflowInput input) {
		stage = "DESIGNING_SOLUTION";
		while (true) {
			status = "RUNNING";
			WorkflowAnalysisResult result = design.design(new WorkflowDesignInput(
					input.workflowId(), input.repositoryUrl(), input.owner(), input.repository(), input.issueNumber(),
					workspacePath, null));
			workspacePath = result.workspacePath();
			status = "WAITING_FOR_USER";
			awaitCommand();
			String command = pendingCommand;
			pendingCommand = null;
			pendingFeedback = null;
			if ("rework".equals(command)) {
				continue;
			}
			stage = "IMPLEMENTING";
			break;
		}
	}

	private void runImplementationStage(WorkflowInput input, String initialFeedback) {
		stage = "IMPLEMENTING";
		String feedback = initialFeedback;
		while (true) {
			status = "RUNNING";
			WorkflowDeveloperResult result = developer.implement(new WorkflowDeveloperInput(
					input.workflowId(), input.repositoryUrl(), input.owner(), input.repository(), input.issueNumber(),
					workspacePath, null, feedback));
			workspacePath = result.workspacePath();
			implementationRunId = result.runId();
			implementationDiagnostics = result.diagnostics();
			feedback = null;
			if (result.success()) {
				stage = "REVIEWING";
				return;
			}
			status = "WAITING_FOR_USER";
			awaitCommand();
			String command = pendingCommand;
			String f = pendingFeedback;
			pendingCommand = null;
			pendingFeedback = null;
			if (!"rework".equals(command)) {
				throw WorkflowActivityException.invalidResult("unexpected command on implementation failure: " + command);
			}
			if (implementationReworkCount >= MAX_REWORK) {
				throw WorkflowActivityException.invalidResult("implementation rework limit exceeded");
			}
			implementationReworkCount++;
			feedback = f;
		}
	}

	private void runReviewGate(WorkflowInput input) {
		stage = "REVIEWING";
		while (true) {
			status = "RUNNING";
			WorkflowReviewerResult result = reviewer.review(new WorkflowReviewerInput(
					input.workflowId(), input.repositoryUrl(), input.owner(), input.repository(), input.issueNumber(),
					workspacePath, implementationDiagnostics));
			reviewRunId = result.runId();
			reviewText = result.reviewText();
			status = "WAITING_FOR_USER";
			awaitCommand();
			String command = pendingCommand;
			String f = pendingFeedback;
			pendingCommand = null;
			pendingFeedback = null;
			if ("rework".equals(command)) {
				if (reviewReworkCount >= MAX_REWORK) {
					throw WorkflowActivityException.invalidResult("review rework limit exceeded");
				}
				reviewReworkCount++;
				reviewText = null;
				runImplementationStage(input, f);
				continue;
			}
			stage = "READY_FOR_PULL_REQUEST";
			break;
		}
	}

	private void runFinalization(WorkflowInput input) {
		stage = "READY_FOR_PULL_REQUEST";
		status = "RUNNING";
		WorkflowFinalizationResult result = finalization.finalize(new WorkflowFinalizationInput(
				input.workflowId(), input.repositoryUrl(), input.owner(), input.repository(), input.issueNumber(),
				branchName, baseBranch));
		commitId = result.commitId();
		branchUrl = result.branchUrl();
		prNumber = result.prNumber();
		prUrl = result.prUrl();
	}

	private void releaseLock() {
		if (lockKey == null || lockToken == null) {
			return;
		}
		try {
			cleanup.releaseLock(lockKey, lockToken);
		} catch (RuntimeException e) {
			// best-effort: терминальный статус не меняется из-за ошибки cleanup.
		}
	}

	private void awaitCommand() {
		Workflow.await(() -> pendingCommand != null);
	}

	@Override
	public void continueStage() {
		pendingCommand = "continue";
	}

	@Override
	public void reworkStage(String feedback) {
		pendingCommand = "rework";
		pendingFeedback = feedback;
	}

	@Override
	public WorkflowStageState state() {
		return new WorkflowStageState(stage, status, workspacePath, branchName,
				implementationRunId, implementationDiagnostics, reviewRunId, reviewText,
				commitId, branchUrl, prNumber, prUrl, failureReason);
	}
}
