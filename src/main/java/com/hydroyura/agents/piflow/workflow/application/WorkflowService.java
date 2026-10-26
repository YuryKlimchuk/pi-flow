package com.hydroyura.agents.piflow.workflow.application;

import java.util.ArrayList;
import java.util.List;

import com.hydroyura.agents.piflow.workspace.api.WorkspaceOperations;
import com.hydroyura.agents.piflow.workflow.api.ContinueWorkflowCommand;
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
 * Реализует публичный порт запуска, чтения состояния и продолжения Workflow.
 *
 * <p>Порядок: create → execution.start. Подготовка workspace и анализ выполняются
 * внутри Temporal execution; актуальный результат execution синхронизируется в
 * состояние Workflow при запросе состояния, а текст артефакта читается из
 * workspace через публичный контракт.</p>
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
				new WorkflowContext(repositoryUrl, command.issueNumber()));

		WorkflowExecutionResult execution;
		try {
			execution = executionPort.start(new WorkflowExecutionCommand(
					command.workflowId(),
					repositoryUrl,
					command.owner(),
					command.repository(),
					command.issueNumber()));
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
		try {
			workflow.continueAfterAnalysis();
		} catch (IllegalWorkflowTransitionException e) {
			throw WorkflowException.illegalTransition(e.getMessage());
		}
		repository.save(workflow);
		return toState(workflow);
	}

	private void syncExecution(Workflow workflow) {
		if (workflow.executionId() == null
				|| workflow.status() == com.hydroyura.agents.piflow.workflow.domain.WorkflowStatus.FAILED
				|| workflow.analysisText() != null) {
			return;
		}
		WorkflowExecutionState state = executionPort.getState(workflow.executionId());
		switch (state.phase()) {
			case RUNNING -> {
				// анализ ещё выполняется — состояние не меняется
			}
			case SUCCEEDED -> {
				try {
					String text = workspaceOperations.readAnalysisArtifact(state.workspacePath()).text();
					workflow.recordAnalysis(state.branchName(), text);
					repository.save(workflow);
				} catch (RuntimeException e) {
					workflow.fail("analysis result unavailable: " + e.getMessage());
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
				workflow.failureReason());
	}
}
