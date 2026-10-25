package com.hydroyura.agents.piflow.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
	private WorkflowExecutionPort executionPort;

	private WorkflowService service;

	@BeforeEach
	void setUp() {
		service = new WorkflowService(executionPort, new InMemoryWorkflowRepository());
	}

	@Test
	void start_shouldCreateThenStartExecution() {
		var command = new StartWorkflowCommand("wf-1", "acme", "demo", 42);
		when(executionPort.start(any())).thenReturn(new WorkflowExecutionResult("exec-1"));

		WorkflowState state = service.start(command);

		assertThat(state.workflowId()).isEqualTo("wf-1");
		assertThat(state.status()).isEqualTo(WorkflowStatus.CREATED);
		assertThat(state.currentStage()).isEqualTo(WorkflowStage.ANALYZING_REQUIREMENTS);
		assertThat(state.executionId()).isEqualTo("exec-1");
		assertThat(state.repositoryUrl()).isEqualTo("https://github.com/acme/demo.git");
		verify(executionPort).start(any(WorkflowExecutionCommand.class));
	}

	@Test
	void start_shouldPassOwnerRepositoryAndIssueNumberToExecution() {
		var command = new StartWorkflowCommand("wf-1", "acme", "demo", 42);
		when(executionPort.start(any())).thenReturn(new WorkflowExecutionResult("exec-1"));

		service.start(command);

		verify(executionPort).start(argThat(cmd ->
				cmd.workflowId().equals("wf-1")
						&& cmd.owner().equals("acme")
						&& cmd.repository().equals("demo")
						&& cmd.issueNumber() == 42
						&& cmd.repositoryUrl().equals("https://github.com/acme/demo.git")));
	}

	@Test
	void start_whenInvalidCommand_shouldRejectBeforeExecution() {
		var command = new StartWorkflowCommand("wf-1", "acme", "demo", 0);

		WorkflowException error = catchThrowableOfType(() -> service.start(command), WorkflowException.class);

		assertThat(error.errorType()).isEqualTo(WorkflowException.ErrorType.VALIDATION);
		verify(executionPort, never()).start(any());
	}

	@Test
	void start_whenExecutionFails_shouldSaveFailedWorkflowAndRethrow() {
		var command = new StartWorkflowCommand("wf-1", "acme", "demo", 42);
		when(executionPort.start(any())).thenThrow(WorkflowException.execution("boom", new RuntimeException()));

		WorkflowException error = catchThrowableOfType(() -> service.start(command), WorkflowException.class);

		assertThat(error.errorType()).isEqualTo(WorkflowException.ErrorType.EXECUTION);
	}

	@Test
	void getState_whenUnknown_shouldThrowNotFound() {
		WorkflowException error = catchThrowableOfType(() -> service.getState("missing"), WorkflowException.class);

		assertThat(error.errorType()).isEqualTo(WorkflowException.ErrorType.NOT_FOUND);
	}
}
