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
 * Реализует публичный порт запуска, чтения состояния, продолжения и rework Workflow.
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

		WorkflowExecutionResult execution;
		try {
			execution = executionPort.start(new WorkflowExecutionCommand(
					command.workflowId(),
					repositoryUrl,
					command.owner(),
					command.repository(),
					command.issueNumber(),
					null,
					null,
					WorkflowExecutionKind.ANALYST));
		} catch (WorkflowException e) {
			workflow.fail("execution start failed");
			repository.save(workflow);
			throw e;
		}
		workflow.attachExecution(execution.executionId());
		workflow.markRunning();
		repository.save(workflow);

		return toState(workflow);
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
		String analysisText = workflow.analysisText();
		try {
			workflow.continueAfterAnalysis();
		} catch (IllegalWorkflowTransitionException e) {
			throw WorkflowException.illegalTransition(e.getMessage());
		}
		try {
			WorkflowExecutionResult execution = executionPort.start(new WorkflowExecutionCommand(
					command.workflowId(),
					workflow.context().repositoryUrl(),
					workflow.context().owner(),
					workflow.context().repository(),
					workflow.context().issueNumber(),
					null,
					analysisText,
					WorkflowExecutionKind.ARCHITECT));
			workflow.attachExecution(execution.executionId());
			repository.save(workflow);
			return toState(workflow);
		} catch (WorkflowException e) {
			workflow.fail("architect start failed");
			repository.save(workflow);
			throw e;
		}
	}

	@Override
	public WorkflowState rework(ReworkWorkflowCommand command) {
		command.validate();
		Workflow workflow = repository.findById(new WorkflowId(command.workflowId()))
				.orElseThrow(() -> WorkflowException.notFound("workflow not found: " + command.workflowId()));
		String previousAnalysis = workflow.analysisText();
		try {
			workflow.rework(command.feedback());
		} catch (IllegalWorkflowTransitionException e) {
			throw WorkflowException.illegalTransition(e.getMessage());
		}
		try {
			WorkflowExecutionResult execution = executionPort.start(new WorkflowExecutionCommand(
					command.workflowId(),
					workflow.context().repositoryUrl(),
					workflow.context().owner(),
					workflow.context().repository(),
					workflow.context().issueNumber(),
					command.feedback(),
					previousAnalysis,
					WorkflowExecutionKind.ANALYST));
			workflow.attachExecution(execution.executionId());
			repository.save(workflow);
			return toState(workflow);
		} catch (WorkflowException e) {
			workflow.fail("rework start failed");
			repository.save(workflow);
			throw e;
		}
	}

	private void syncExecution(Workflow workflow) {
		if (workflow.executionId() == null
				|| workflow.status() == com.hydroyura.agents.piflow.workflow.domain.WorkflowStatus.FAILED) {
			return;
		}
		com.hydroyura.agents.piflow.workflow.domain.WorkflowStage stage = workflow.currentStage();
		if (stage == com.hydroyura.agents.piflow.workflow.domain.WorkflowStage.ANALYZING_REQUIREMENTS
				&& workflow.analysisText() != null) {
			return;
		}
		if (stage == com.hydroyura.agents.piflow.workflow.domain.WorkflowStage.DESIGNING_SOLUTION
				&& workflow.designText() != null) {
			return;
		}
		if (stage != com.hydroyura.agents.piflow.workflow.domain.WorkflowStage.ANALYZING_REQUIREMENTS
				&& stage != com.hydroyura.agents.piflow.workflow.domain.WorkflowStage.DESIGNING_SOLUTION) {
			return;
		}
		WorkflowExecutionState state = executionPort.getState(workflow.executionId());
		switch (state.phase()) {
			case RUNNING -> {
				// этап ещё выполняется
			}
			case SUCCEEDED -> {
				try {
					if (stage == com.hydroyura.agents.piflow.workflow.domain.WorkflowStage.DESIGNING_SOLUTION) {
						String text = workspaceOperations.readDesignArtifact(state.workspacePath()).text();
						workflow.recordDesign(text);
					} else {
						String text = workspaceOperations.readAnalysisArtifact(state.workspacePath()).text();
						workflow.recordAnalysis(state.branchName(), text);
					}
					repository.save(workflow);
				} catch (RuntimeException e) {
					workflow.fail("result unavailable: " + e.getMessage());
					repository.save(workflow);
				}
			}
			case FAILED -> {
				workflow.fail(state.errorMessage());
				repository.save(workflow);
			}
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
				workflow.failureReason());
	}
}
