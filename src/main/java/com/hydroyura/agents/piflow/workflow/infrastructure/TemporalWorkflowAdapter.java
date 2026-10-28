package com.hydroyura.agents.piflow.workflow.infrastructure;

import com.hydroyura.agents.piflow.workflow.application.WorkflowExecutionCommand;
import com.hydroyura.agents.piflow.workflow.application.WorkflowExecutionPort;
import com.hydroyura.agents.piflow.workflow.application.WorkflowExecutionResult;
import com.hydroyura.agents.piflow.workflow.application.WorkflowExecutionState;
import com.hydroyura.agents.piflow.workflow.api.WorkflowException;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.PiFlowWorkflow;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowInput;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowStageState;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;

import org.springframework.stereotype.Component;

/**
 * Адаптер управления единым длительным Temporal execution.
 */
@Component
public class TemporalWorkflowAdapter implements WorkflowExecutionPort {

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
			WorkflowStub.fromTyped(workflow).start(new WorkflowInput(
					command.workflowId(),
					command.repositoryUrl(),
					command.owner(),
					command.repository(),
					command.issueNumber(),
					null,
					null));
			return new WorkflowExecutionResult(command.workflowId());
		} catch (io.temporal.client.WorkflowException e) {
			throw WorkflowException.execution("temporal execution failed to start: " + e.getMessage(), e);
		} catch (RuntimeException e) {
			throw WorkflowException.execution("temporal execution start failed: " + e.getMessage(), e);
		}
	}

	@Override
	public WorkflowExecutionState queryState(String executionId) {
		WorkflowStub stub = workflowClient.newUntypedWorkflowStub(executionId);
		try {
			WorkflowStageState state = stub.query("state", WorkflowStageState.class);
			return new WorkflowExecutionState(
					state.stage(), state.status(), state.workspacePath(), state.branchName(), state.failureReason(),
					state.implementationRunId(), state.implementationDiagnostics(),
					state.reviewRunId(), state.reviewText(),
					state.commitId(), state.branchUrl(), state.prNumber(), state.prUrl());
		} catch (io.temporal.client.WorkflowException e) {
			throw WorkflowException.execution("temporal query failed: " + e.getMessage(), e);
		}
	}

	@Override
	public void continueStage(String executionId) {
		signal(executionId, "continueStage");
	}

	@Override
	public void reworkStage(String executionId, String feedback) {
		signal(executionId, "reworkStage", feedback);
	}

	private void signal(String executionId, String signalName, Object... args) {
		WorkflowStub stub = workflowClient.newUntypedWorkflowStub(executionId);
		try {
			stub.signal(signalName, args);
		} catch (io.temporal.client.WorkflowException e) {
			throw WorkflowException.execution("temporal signal failed: " + e.getMessage(), e);
		}
	}
}
