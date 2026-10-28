package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Map;

import com.hydroyura.agents.piflow.agent.api.AgentOperations;
import com.hydroyura.agents.piflow.agent.api.AgentRole;
import com.hydroyura.agents.piflow.agent.api.AgentRunResult;
import com.hydroyura.agents.piflow.agent.api.AgentRunStatus;
import com.hydroyura.agents.piflow.agent.api.StartAgentCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit-тест activity реализации Developer.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowDeveloperActivitiesImplTest {

	@TempDir
	Path workspace;

	@Mock
	private AgentOperations agentOperations;

	private WorkflowDeveloperActivitiesImpl activity;

	@BeforeEach
	void setUp() {
		activity = new WorkflowDeveloperActivitiesImpl(agentOperations);
	}

	@Test
	void implement_whenSuccess_shouldReturnSuccess() {
		when(agentOperations.run(any())).thenReturn(
				new AgentRunResult("run-1", AgentRole.DEVELOPER, AgentRunStatus.SUCCEEDED, "build ok", "", 0, Map.of()));

		WorkflowDeveloperResult result = activity.implement(input());

		assertThat(result.success()).isTrue();
		assertThat(result.runId()).isEqualTo("run-1");
		assertThat(result.diagnostics()).contains("build ok");

		ArgumentCaptor<StartAgentCommand> captor = ArgumentCaptor.forClass(StartAgentCommand.class);
		verify(agentOperations).run(captor.capture());
		assertThat(captor.getValue().role()).isEqualTo(AgentRole.DEVELOPER);
		assertThat(captor.getValue().workingDirectory()).isEqualTo(workspace);
	}

	@Test
	void implement_whenNonZeroExit_shouldReturnFailure() {
		when(agentOperations.run(any())).thenReturn(
				new AgentRunResult("run-1", AgentRole.DEVELOPER, AgentRunStatus.SUCCEEDED, "", "tests failed", 1, Map.of()));

		WorkflowDeveloperResult result = activity.implement(input());

		assertThat(result.success()).isFalse();
		assertThat(result.diagnostics()).contains("tests failed");
	}

	@Test
	void implement_whenTimedOut_shouldReturnFailure() {
		when(agentOperations.run(any())).thenReturn(
				new AgentRunResult("run-1", AgentRole.DEVELOPER, AgentRunStatus.TIMED_OUT, "", "", null, Map.of()));

		WorkflowDeveloperResult result = activity.implement(input());

		assertThat(result.success()).isFalse();
	}

	@Test
	void implement_whenMissingWorkspace_shouldThrowInvalidResult() {
		WorkflowActivityException error = catchThrowableOfType(
				() -> activity.implement(new WorkflowDeveloperInput("wf-1", "url", "acme", "demo", 42, null, null, null)),
				WorkflowActivityException.class);

		assertThat(error.reason()).isEqualTo(WorkflowActivityException.Reason.INVALID_RESULT);
	}

	@Test
	void implement_whenLaunchError_shouldThrowAgentFailed() {
		when(agentOperations.run(any())).thenThrow(new RuntimeException("executable missing"));

		WorkflowActivityException error = catchThrowableOfType(() -> activity.implement(input()),
				WorkflowActivityException.class);

		assertThat(error.reason()).isEqualTo(WorkflowActivityException.Reason.AGENT_FAILED);
	}

	private WorkflowDeveloperInput input() {
		return new WorkflowDeveloperInput("wf-1", "https://github.com/acme/demo.git", "acme", "demo", 42,
				workspace.toString(), "design", null);
	}
}
