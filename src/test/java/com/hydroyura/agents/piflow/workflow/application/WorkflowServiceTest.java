package com.hydroyura.agents.piflow.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import com.hydroyura.agents.piflow.agent.api.AgentOperations;
import com.hydroyura.agents.piflow.agent.api.AgentRunResult;
import com.hydroyura.agents.piflow.agent.api.AgentRunStatus;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceException;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspacePreparationRequest;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceState;

import com.hydroyura.agents.piflow.workflow.api.StartWorkflowCommand;
import com.hydroyura.agents.piflow.workflow.api.WorkflowException;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.api.WorkflowState;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStatus;
import com.hydroyura.agents.piflow.workflow.infrastructure.InMemoryWorkflowRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

	@Mock
	private WorkspaceOperations workspaceOperations;

	@Mock
	private AgentOperations agentOperations;

	@Mock
	private WorkflowExecutionPort executionPort;

	private WorkflowService service;

	@BeforeEach
	void setUp() {
		service = new WorkflowService(workspaceOperations, agentOperations, executionPort,
				new InMemoryWorkflowRepository());
	}

	@Test
	void start_shouldPrepareWorkspaceThenRunAgentThenStartExecution() {
		var command = new StartWorkflowCommand("wf-1", "https://github.com/o/r", 42);
		when(workspaceOperations.prepare(any())).thenReturn(
				new WorkspaceResult(Path.of("/tmp/ws"), "https://github.com/o/r", "pi-flow/wf-1", WorkspaceState.CLEAN));
		when(agentOperations.run(any())).thenReturn(
				new AgentRunResult("run-1", com.hydroyura.agents.piflow.agent.api.AgentRole.ANALYST,
						AgentRunStatus.SUCCEEDED, "out", "", 0, java.util.Map.of()));
		when(executionPort.start(any())).thenReturn(new WorkflowExecutionResult("exec-1"));

		WorkflowState state = service.start(command);

		assertThat(state.workflowId()).isEqualTo("wf-1");
		assertThat(state.status()).isEqualTo(WorkflowStatus.CREATED);
		assertThat(state.currentStage()).isEqualTo(WorkflowStage.ANALYZING_REQUIREMENTS);
		assertThat(state.executionId()).isEqualTo("exec-1");
		verify(workspaceOperations).prepare(any(WorkspacePreparationRequest.class));
		verify(agentOperations).run(any());
		verify(executionPort).start(any(WorkflowExecutionCommand.class));
	}

	@Test
	void start_whenWorkspaceFails_shouldNotRunAgentOrExecution() {
		var command = new StartWorkflowCommand("wf-1", "https://github.com/o/r", 42);
		when(workspaceOperations.prepare(any())).thenThrow(WorkspaceException.dirty("dirty"));

		WorkflowException error = catchThrowableOfType(() -> service.start(command), WorkflowException.class);

		assertThat(error.errorType()).isEqualTo(WorkflowException.ErrorType.ORCHESTRATION);
		verify(agentOperations, never()).run(any());
		verify(executionPort, never()).start(any());
	}

	@Test
	void start_whenInvalidCommand_shouldRejectBeforeAnyOperation() {
		var command = new StartWorkflowCommand("wf-1", "https://github.com/o/r", 0);

		WorkflowException error = catchThrowableOfType(() -> service.start(command), WorkflowException.class);

		assertThat(error.errorType()).isEqualTo(WorkflowException.ErrorType.VALIDATION);
		verify(workspaceOperations, never()).prepare(any());
	}

	@Test
	void getState_whenUnknown_shouldThrowNotFound() {
		WorkflowException error = catchThrowableOfType(() -> service.getState("missing"), WorkflowException.class);

		assertThat(error.errorType()).isEqualTo(WorkflowException.ErrorType.NOT_FOUND);
	}
}
