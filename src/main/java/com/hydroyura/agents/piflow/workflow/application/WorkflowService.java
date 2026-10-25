package com.hydroyura.agents.piflow.workflow.application;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.hydroyura.agents.piflow.agent.api.AgentOperations;
import com.hydroyura.agents.piflow.agent.api.AgentRole;
import com.hydroyura.agents.piflow.agent.api.StartAgentCommand;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspacePreparationRequest;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceResult;

import com.hydroyura.agents.piflow.workflow.api.StartWorkflowCommand;
import com.hydroyura.agents.piflow.workflow.api.WorkflowException;
import com.hydroyura.agents.piflow.workflow.api.WorkflowOperations;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.api.WorkflowState;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStatus;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import com.hydroyura.agents.piflow.workflow.domain.WorkflowContext;
import com.hydroyura.agents.piflow.workflow.domain.WorkflowId;

import org.springframework.stereotype.Service;

/**
 * Реализует публичный порт запуска Workflow и минимальный сквозной сценарий.
 *
 * <p>Порядок: create → workspace.prepare → agent.run → execution.start.
 * Адресные операции делегируются публичным контрактам соседних модулей.</p>
 */
@Service
public class WorkflowService implements WorkflowOperations {

	private final WorkspaceOperations workspaceOperations;
	private final AgentOperations agentOperations;
	private final WorkflowExecutionPort executionPort;
	private final WorkflowRepository repository;

	public WorkflowService(
			WorkspaceOperations workspaceOperations,
			AgentOperations agentOperations,
			WorkflowExecutionPort executionPort,
			WorkflowRepository repository) {
		this.workspaceOperations = workspaceOperations;
		this.agentOperations = agentOperations;
		this.executionPort = executionPort;
		this.repository = repository;
	}

	@Override
	public WorkflowState start(StartWorkflowCommand command) {
		command.validate();
		Workflow workflow = Workflow.create(
				new WorkflowId(command.workflowId()),
				new WorkflowContext(command.repositoryUrl(), command.issueNumber()));

		// 1. prepare workspace
		WorkspaceResult workspace;
		try {
			workspace = workspaceOperations.prepare(
					new WorkspacePreparationRequest(
							command.workflowId(),
							command.repositoryUrl(),
							"main",
							null));
		} catch (RuntimeException e) {
			workflow.fail("workspace preparation failed");
			repository.save(workflow);
			throw WorkflowException.orchestration("workspace preparation failed", e);
		}

		// 2. run agent
		try {
			agentOperations.run(new StartAgentCommand(
					AgentRole.ANALYST,
					"Analyze issue #" + command.issueNumber(),
					null,
					command.repositoryUrl(),
					workspace.path(),
					null,
					null));
		} catch (RuntimeException e) {
			workflow.fail("agent run failed");
			repository.save(workflow);
			throw WorkflowException.orchestration("agent run failed", e);
		}

		// 3. start execution
		WorkflowExecutionResult execution;
		try {
			execution = executionPort.start(
					new WorkflowExecutionCommand(command.workflowId(), command.repositoryUrl(), command.issueNumber()));
		} catch (WorkflowException e) {
			workflow.fail("execution start failed");
			repository.save(workflow);
			throw e;
		}
		workflow.attachExecution(execution.executionId());
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
		return toState(workflow);
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
				workflow.executionId());
	}
}
