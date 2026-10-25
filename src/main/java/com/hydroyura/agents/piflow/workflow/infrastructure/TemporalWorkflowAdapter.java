package com.hydroyura.agents.piflow.workflow.infrastructure;

import com.hydroyura.agents.piflow.workflow.application.WorkflowExecutionCommand;
import com.hydroyura.agents.piflow.workflow.application.WorkflowExecutionPort;
import com.hydroyura.agents.piflow.workflow.application.WorkflowExecutionResult;
import com.hydroyura.agents.piflow.workflow.api.WorkflowException;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.PiFlowWorkflow;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowInput;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;

import org.springframework.stereotype.Component;

/**
 * Адаптер исполнения Workflow через Temporal.
 *
 * <p>Преобразует application-команду в Temporal workflow и транслирует ошибки SDK
 * в типизированные ошибки контракта workflow, не допуская утечки типов SDK.</p>
 */
@Component
public class TemporalWorkflowAdapter implements WorkflowExecutionPort {

	private static final String WORKFLOW_TYPE = "PiFlowWorkflow";

	private final WorkflowClient workflowClient;
	private final TemporalProperties properties;

	public TemporalWorkflowAdapter(WorkflowClient workflowClient, TemporalProperties properties) {
		this.workflowClient = workflowClient;
		this.properties = properties;
	}

	@Override
	public WorkflowExecutionResult start(WorkflowExecutionCommand command) {
		WorkflowOptions options = WorkflowOptions.newBuilder()
				.setWorkflowId(command.workflowId())
				.setTaskQueue(properties.taskQueue())
				.build();

		try {
			PiFlowWorkflow workflow = workflowClient.newWorkflowStub(PiFlowWorkflow.class, options);
			WorkflowStub stub = WorkflowStub.fromTyped(workflow);
			stub.start(new WorkflowInput(
					command.workflowId(),
					command.repositoryUrl(),
					command.owner(),
					command.repository(),
					command.issueNumber()));
			return new WorkflowExecutionResult(command.workflowId());
		} catch (io.temporal.client.WorkflowException e) {
			throw WorkflowException.execution("temporal execution failed to start: " + e.getMessage(), e);
		} catch (RuntimeException e) {
			throw WorkflowException.execution("temporal execution start failed: " + e.getMessage(), e);
		}
	}
}
