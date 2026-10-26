package com.hydroyura.agents.piflow.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hydroyura.agents.piflow.workspace.api.WorkspaceArtifactResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceOperations;
import com.hydroyura.agents.piflow.workflow.api.ContinueWorkflowCommand;
import com.hydroyura.agents.piflow.workflow.api.ReworkWorkflowCommand;
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

	@Mock
	private WorkspaceOperations workspaceOperations;

	private WorkflowService service;

	@BeforeEach
	void setUp() {
		service = new WorkflowService(executionPort, new InMemoryWorkflowRepository(), workspaceOperations);
	}

	@Test
	void start_shouldCreateThenStartExecution() {
		when(executionPort.start(any())).thenReturn(new WorkflowExecutionResult("wf-1"));
		StartWorkflowCommand command = new StartWorkflowCommand("wf-1", "acme", "demo", 42);

		WorkflowState state = service.start(command);

		assertThat(state.workflowId()).isEqualTo("wf-1");
		assertThat(state.status()).isEqualTo(WorkflowStatus.RUNNING);
		assertThat(state.executionId()).isEqualTo("wf-1");
		verify(executionPort).start(any(WorkflowExecutionCommand.class));
	}

	@Test
	void getState_whenRunning_shouldNotRecordResult() {
		startWorkflow();
		when(executionPort.queryState("wf-1")).thenReturn(
				new WorkflowExecutionState("ANALYZING_REQUIREMENTS", "RUNNING", null, null, null));

		WorkflowState state = service.getState("wf-1");

		assertThat(state.status()).isEqualTo(WorkflowStatus.RUNNING);
		assertThat(state.analysisText()).isNull();
	}

	@Test
	void getState_whenWaitingAnalysis_shouldRecordAnalysis() {
		startWorkflow();
		when(executionPort.queryState("wf-1")).thenReturn(
				new WorkflowExecutionState("ANALYZING_REQUIREMENTS", "WAITING_FOR_USER", "/ws", "feature/issue-42", null));
		when(workspaceOperations.readAnalysisArtifact("/ws")).thenReturn(
				new WorkspaceArtifactResult("analysis text", "/ws/x", 13));

		WorkflowState state = service.getState("wf-1");

		assertThat(state.status()).isEqualTo(WorkflowStatus.WAITING_FOR_USER);
		assertThat(state.analysisText()).isEqualTo("analysis text");
		assertThat(state.branchName()).isEqualTo("feature/issue-42");
	}

	@Test
	void getState_whenFailed_shouldMarkFailed() {
		startWorkflow();
		when(executionPort.queryState("wf-1")).thenReturn(
				new WorkflowExecutionState("ANALYZING_REQUIREMENTS", "FAILED", null, null, "boom"));

		WorkflowState state = service.getState("wf-1");

		assertThat(state.status()).isEqualTo(WorkflowStatus.FAILED);
		assertThat(state.errorMessage()).isEqualTo("boom");
	}

	@Test
	void continueWorkflow_shouldSignalAndAdvanceStage() {
		recordAnalysis();

		WorkflowState state = service.continueWorkflow(new ContinueWorkflowCommand("wf-1", "feature/issue-42"));

		assertThat(state.currentStage()).isEqualTo(WorkflowStage.DESIGNING_SOLUTION);
		assertThat(state.analysisText()).isNull();
		verify(executionPort).continueStage("wf-1");
	}

	@Test
	void rework_shouldSignalAndReset() {
		recordAnalysis();

		WorkflowState state = service.rework(new ReworkWorkflowCommand("wf-1", "добавь риски"));

		assertThat(state.currentStage()).isEqualTo(WorkflowStage.ANALYZING_REQUIREMENTS);
		assertThat(state.analysisText()).isNull();
		verify(executionPort).reworkStage("wf-1", "добавь риски");
	}

	@Test
	void rework_whenBlankFeedback_shouldReject() {
		startWorkflow();

		WorkflowException error = catchThrowableOfType(
				() -> service.rework(new ReworkWorkflowCommand("wf-1", "  ")), WorkflowException.class);

		assertThat(error.errorType()).isEqualTo(WorkflowException.ErrorType.VALIDATION);
		verify(executionPort, never()).reworkStage(any(), any());
	}

	private void startWorkflow() {
		when(executionPort.start(any())).thenReturn(new WorkflowExecutionResult("wf-1"));
		service.start(new StartWorkflowCommand("wf-1", "acme", "demo", 42));
	}

	private void recordAnalysis() {
		startWorkflow();
		when(executionPort.queryState("wf-1")).thenReturn(
				new WorkflowExecutionState("ANALYZING_REQUIREMENTS", "WAITING_FOR_USER", "/ws", "feature/issue-42", null));
		when(workspaceOperations.readAnalysisArtifact("/ws")).thenReturn(
				new WorkspaceArtifactResult("analysis text", "/ws/x", 13));
		service.getState("wf-1");
	}
}
