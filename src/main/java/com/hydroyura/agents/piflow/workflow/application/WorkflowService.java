package com.hydroyura.agents.piflow.workflow.application;

import java.util.ArrayList;
import java.util.List;

import com.hydroyura.agents.piflow.workspace.api.WorkspaceOperations;
import com.hydroyura.agents.piflow.workflow.api.ContinueWorkflowCommand;
import com.hydroyura.agents.piflow.workflow.api.ReworkWorkflowCommand;
import com.hydroyura.agents.piflow.workflow.api.StartWorkflowCommand;
import com.hydroyura.agents.piflow.workflow.api.WorkflowException;
import com.hydroyura.agents.piflow.workflow.api.WorkflowOperations;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.api.WorkflowState;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStatus;
import com.hydroyura.agents.piflow.workflow.domain.IllegalWorkflowTransitionException;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import com.hydroyura.agents.piflow.workflow.domain.WorkflowContext;
import com.hydroyura.agents.piflow.workflow.domain.WorkflowId;

import org.springframework.stereotype.Service;

/**
 * Реализует публичный порт управления единым длительным Workflow.
 */
@Service
public class WorkflowService implements WorkflowOperations {

	private final WorkflowExecutionPort executionPort;
	private final WorkflowRepository repository;
	private final WorkspaceOperations workspaceOperations;

	public WorkflowService(
			WorkflowExecutionPort executionPort,
			WorkflowRepository repository,
			WorkspaceOperations workspaceOperations) {
		this.executionPort = executionPort;
		this.repository = repository;
		this.workspaceOperations = workspaceOperations;
	}

	@Override
	public WorkflowState start(StartWorkflowCommand command) {
		command.validate();
		String repositoryUrl = cloneUrl(command.owner(), command.repository());
		Workflow workflow = Workflow.create(
				new WorkflowId(command.workflowId()),
				new WorkflowContext(repositoryUrl, command.owner(), command.repository(), command.issueNumber()));
		try {
			WorkflowExecutionResult execution = executionPort.start(new WorkflowExecutionCommand(
					command.workflowId(), repositoryUrl, command.owner(), command.repository(), command.issueNumber()));
			workflow.attachExecution(execution.executionId());
			workflow.markRunning();
			repository.save(workflow);
			return toState(workflow);
		} catch (WorkflowException e) {
			workflow.fail("execution start failed");
			repository.save(workflow);
			throw e;
		}
	}

	@Override
	public WorkflowState getState(String workflowId) {
		if (workflowId == null || workflowId.isBlank()) {
			throw WorkflowException.validation("workflowId must not be blank");
		}
		Workflow workflow = repository.findById(new WorkflowId(workflowId))
				.orElseThrow(() -> WorkflowException.notFound("workflow not found: " + workflowId));
		syncExecution(workflow);
		return toState(workflow);
	}

	@Override
	public WorkflowState continueWorkflow(ContinueWorkflowCommand command) {
		command.validate();
		Workflow workflow = repository.findById(new WorkflowId(command.workflowId()))
				.orElseThrow(() -> WorkflowException.notFound("workflow not found: " + command.workflowId()));
		try {
			workflow.continueAfterAnalysis();
		} catch (IllegalWorkflowTransitionException e) {
			throw WorkflowException.illegalTransition(e.getMessage());
		}
		executionPort.continueStage(workflow.executionId());
		repository.save(workflow);
		return toState(workflow);
	}

	@Override
	public WorkflowState rework(ReworkWorkflowCommand command) {
		command.validate();
		Workflow workflow = repository.findById(new WorkflowId(command.workflowId()))
				.orElseThrow(() -> WorkflowException.notFound("workflow not found: " + command.workflowId()));
		try {
			workflow.rework(command.feedback());
		} catch (IllegalWorkflowTransitionException e) {
			throw WorkflowException.illegalTransition(e.getMessage());
		}
		executionPort.reworkStage(workflow.executionId(), command.feedback());
		repository.save(workflow);
		return toState(workflow);
	}

	@Override
	public java.util.List<com.hydroyura.agents.piflow.workflow.api.WorkflowSummary> listWorkflows() {
		return repository.findAll().stream()
				.map(w -> new com.hydroyura.agents.piflow.workflow.api.WorkflowSummary(
						w.id().value(),
						w.context().repositoryUrl(),
						w.context().issueNumber(),
						w.status().name(),
						w.currentStage().name()))
				.toList();
	}

	private void syncExecution(Workflow workflow) {
		if (workflow.executionId() == null
				|| workflow.status() == com.hydroyura.agents.piflow.workflow.domain.WorkflowStatus.FAILED) {
			return;
		}
		WorkflowExecutionState state = executionPort.queryState(workflow.executionId());
		if ("FAILED".equals(state.status())) {
			workflow.fail(state.failureReason());
			repository.save(workflow);
			return;
		}
		if (!"WAITING_FOR_USER".equals(state.status())) {
			return;
		}
		try {
			if ("ANALYZING_REQUIREMENTS".equals(state.stage()) && workflow.analysisText() == null) {
				String text = workspaceOperations.readAnalysisArtifact(state.workspacePath()).text();
				workflow.recordAnalysis(state.branchName(), text);
			} else if ("DESIGNING_SOLUTION".equals(state.stage()) && workflow.designText() == null) {
				String text = workspaceOperations.readDesignArtifact(state.workspacePath()).text();
				workflow.recordDesign(text);
			}
			repository.save(workflow);
		} catch (RuntimeException e) {
			workflow.fail("result unavailable: " + e.getMessage());
			repository.save(workflow);
		}
	}

	private String cloneUrl(String owner, String repository) {
		return "https://github.com/" + owner + "/" + repository + ".git";
	}

	private WorkflowState toState(Workflow workflow) {
		List<WorkflowStage> completed = new ArrayList<>();
		for (com.hydroyura.agents.piflow.workflow.domain.WorkflowStage stage : workflow.completedStages()) {
			completed.add(WorkflowStage.valueOf(stage.name()));
		}
		List<com.hydroyura.agents.piflow.workflow.api.WorkflowStepInfo> timeline = new ArrayList<>();
		for (com.hydroyura.agents.piflow.workflow.domain.CompletedWorkflowStep step : workflow.timeline()) {
			timeline.add(new com.hydroyura.agents.piflow.workflow.api.WorkflowStepInfo(
					step.stage(), step.role(), step.action(), step.result(), step.runId()));
		}
		return new WorkflowState(
				workflow.id().value(),
				workflow.context().repositoryUrl(),
				workflow.context().issueNumber(),
				WorkflowStatus.valueOf(workflow.status().name()),
				WorkflowStage.valueOf(workflow.currentStage().name()),
				List.copyOf(completed),
				workflow.executionId(),
				workflow.analysisText(),
				workflow.branchName(),
				workflow.designText(),
				workflow.failureReason(),
				List.copyOf(timeline));
	}
}
