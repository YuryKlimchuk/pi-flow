package com.hydroyura.agents.piflow.workflow.infrastructure;

import com.hydroyura.agents.piflow.workflow.application.WorkflowExecutionCommand;
import com.hydroyura.agents.piflow.workflow.application.WorkflowExecutionKind;
import com.hydroyura.agents.piflow.workflow.application.WorkflowExecutionPort;
import com.hydroyura.agents.piflow.workflow.application.WorkflowExecutionResult;
import com.hydroyura.agents.piflow.workflow.application.WorkflowExecutionState;
import com.hydroyura.agents.piflow.workflow.api.WorkflowException;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.PiFlowArchitectWorkflow;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.PiFlowWorkflow;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowAnalysisResult;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowInput;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
		String executionId = command.workflowId() + "-" + command.kind().name().toLowerCase() + "-" + UUID.randomUUID();
		WorkflowOptions options = WorkflowOptions.newBuilder()
				.setWorkflowId(executionId)
				.setTaskQueue(properties.taskQueue())
				.build();

		try {
			WorkflowInput input = new WorkflowInput(
					command.workflowId(),
					command.repositoryUrl(),
					command.owner(),
					command.repository(),
					command.issueNumber(),
					command.feedback(),
					command.analysisText());
			if (command.kind() == WorkflowExecutionKind.ARCHITECT) {
				PiFlowArchitectWorkflow workflow = workflowClient.newWorkflowStub(PiFlowArchitectWorkflow.class, options);
				WorkflowStub.fromTyped(workflow).start(input);
			} else {
				PiFlowWorkflow workflow = workflowClient.newWorkflowStub(PiFlowWorkflow.class, options);
				WorkflowStub.fromTyped(workflow).start(input);
			}
			return new WorkflowExecutionResult(executionId);
		} catch (io.temporal.client.WorkflowException e) {
			throw WorkflowException.execution("temporal execution failed to start: " + e.getMessage(), e);
		} catch (RuntimeException e) {
			throw WorkflowException.execution("temporal execution start failed: " + e.getMessage(), e);
		}
	}

	@Override
	public WorkflowExecutionState getState(String executionId) {
		WorkflowStub stub = workflowClient.newUntypedWorkflowStub(executionId);
		try {
			WorkflowAnalysisResult result = stub.getResult(500, TimeUnit.MILLISECONDS, WorkflowAnalysisResult.class);
			return WorkflowExecutionState.succeeded(result.workspacePath(), result.branchName());
		} catch (TimeoutException e) {
			return WorkflowExecutionState.running();
		} catch (io.temporal.client.WorkflowException e) {
			return WorkflowExecutionState.failed(e.getMessage());
		}
	}
}
