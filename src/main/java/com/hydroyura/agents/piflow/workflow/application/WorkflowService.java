package com.hydroyura.agents.piflow.workflow.application;

import java.util.ArrayList;
import java.util.List;

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
 * Реализует публичный порт запуска Workflow и минимальный сценарий.
 *
 * <p>Порядок: create → execution.start. Подготовка workspace выполняется внутри
 * Temporal execution через activity; запуск Pi-агента в этом сценарии НЕ
 * происходит. URL репозитория вычисляется на application boundary из
 * owner/repository, а не в web-слое или в модуле workspace.</p>
 */
@Service
public class WorkflowService implements WorkflowOperations {

	private final WorkflowExecutionPort executionPort;
	private final WorkflowRepository repository;

	public WorkflowService(
			WorkflowExecutionPort executionPort,
			WorkflowRepository repository) {
		this.executionPort = executionPort;
		this.repository = repository;
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
				workflow.executionId());
	}
}
