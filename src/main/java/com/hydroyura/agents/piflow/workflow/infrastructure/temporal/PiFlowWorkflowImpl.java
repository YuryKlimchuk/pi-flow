package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * Реализация единого длинного workflow: preparation → Analyst (gate) →
 * Architect (gate). Ручные этапы ждут сигнала continue/rework через
 * {@link Workflow#await}.
 */
public class PiFlowWorkflowImpl implements PiFlowWorkflow {

	private static final RetryOptions RETRY_OPTIONS = RetryOptions.newBuilder()
			.setMaximumAttempts(3)
			.setInitialInterval(Duration.ofSeconds(1))
			.setBackoffCoefficient(2.0)
			.setMaximumInterval(Duration.ofSeconds(10))
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

	private String stage = "ANALYZING_REQUIREMENTS";
	private String status = "RUNNING";
	private String workspacePath;
	private String branchName;
	private String analysisArtifactPath;
	private String designArtifactPath;
	private String failureReason;
	private String pendingCommand;
	private String pendingFeedback;

	@Override
	public void run(WorkflowInput input) {
		WorkflowPreparationResult prepared = preparation.prepare(input);
		workspacePath = prepared.path();

		runAnalystGate(input);
		runArchitectGate(input);

		status = "COMPLETED";
	}

	private void runAnalystGate(WorkflowInput input) {
		stage = "ANALYZING_REQUIREMENTS";
		status = "RUNNING";
		String previousAnalysis = null;
		while (true) {
			WorkflowAnalysisResult result = analysis.analyze(new WorkflowAnalysisInput(
					input.workflowId(), input.repositoryUrl(), input.owner(), input.repository(), input.issueNumber(),
					workspacePath, pendingFeedback, previousAnalysis));
			workspacePath = result.workspacePath();
			branchName = result.branchName();
			analysisArtifactPath = result.workspacePath();
			status = "WAITING_FOR_USER";
			pendingCommand = null;
			pendingFeedback = null;
			Workflow.await(() -> pendingCommand != null);
			String command = pendingCommand;
			String feedback = pendingFeedback;
			pendingCommand = null;
			if ("rework".equals(command)) {
				status = "RUNNING";
				previousAnalysis = null;
				pendingFeedback = feedback;
				continue;
			}
			break;
		}
	}

	private void runArchitectGate(WorkflowInput input) {
		stage = "DESIGNING_SOLUTION";
		status = "RUNNING";
		pendingCommand = null;
		pendingFeedback = null;
		while (true) {
			WorkflowAnalysisResult result = design.design(new WorkflowDesignInput(
					input.workflowId(), input.repositoryUrl(), input.owner(), input.repository(), input.issueNumber(),
					workspacePath, null));
			workspacePath = result.workspacePath();
			designArtifactPath = result.workspacePath();
			status = "WAITING_FOR_USER";
			pendingCommand = null;
			pendingFeedback = null;
			Workflow.await(() -> pendingCommand != null);
			String command = pendingCommand;
			String feedback = pendingFeedback;
			pendingCommand = null;
			if ("rework".equals(command)) {
				status = "RUNNING";
				pendingFeedback = feedback;
				continue;
			}
			stage = "IMPLEMENTING";
			break;
		}
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
		return new WorkflowStageState(stage, status, workspacePath, branchName, analysisArtifactPath,
				designArtifactPath, failureReason);
	}
}
