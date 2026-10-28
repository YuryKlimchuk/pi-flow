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
 * Unit-тест activity независимого ревью Reviewer.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowReviewerActivitiesImplTest {

	@TempDir
	Path workspace;

	@Mock
	private AgentOperations agentOperations;

	private WorkflowReviewerActivitiesImpl activity;

	@BeforeEach
	void setUp() {
		activity = new WorkflowReviewerActivitiesImpl(agentOperations);
	}

	@Test
	void review_whenSuccessful_shouldReturnReviewText() {
		when(agentOperations.run(any())).thenReturn(
				new AgentRunResult("run-1", AgentRole.REVIEWER, AgentRunStatus.SUCCEEDED, "замечания к коду", "", 0, Map.of()));

		WorkflowReviewerResult result = activity.review(input());

		assertThat(result.runId()).isEqualTo("run-1");
		assertThat(result.reviewText()).isEqualTo("замечания к коду");

		ArgumentCaptor<StartAgentCommand> captor = ArgumentCaptor.forClass(StartAgentCommand.class);
		verify(agentOperations).run(captor.capture());
		assertThat(captor.getValue().role()).isEqualTo(AgentRole.REVIEWER);
	}

	@Test
	void review_whenFailed_shouldThrowAgentFailed() {
		when(agentOperations.run(any())).thenReturn(
				new AgentRunResult("run-1", AgentRole.REVIEWER, AgentRunStatus.FAILED, "", "err", 1, Map.of()));

		WorkflowActivityException error = catchThrowableOfType(() -> activity.review(input()),
				WorkflowActivityException.class);

		assertThat(error.reason()).isEqualTo(WorkflowActivityException.Reason.AGENT_FAILED);
	}

	@Test
	void review_whenEmptyResult_shouldThrowInvalidResult() {
		when(agentOperations.run(any())).thenReturn(
				new AgentRunResult("run-1", AgentRole.REVIEWER, AgentRunStatus.SUCCEEDED, "   ", "", 0, Map.of()));

		WorkflowActivityException error = catchThrowableOfType(() -> activity.review(input()),
				WorkflowActivityException.class);

		assertThat(error.reason()).isEqualTo(WorkflowActivityException.Reason.INVALID_RESULT);
	}

	@Test
	void review_whenMissingWorkspace_shouldThrowInvalidResult() {
		WorkflowActivityException error = catchThrowableOfType(
				() -> activity.review(new WorkflowReviewerInput("wf-1", "url", "acme", "demo", 42, null, null)),
				WorkflowActivityException.class);

		assertThat(error.reason()).isEqualTo(WorkflowActivityException.Reason.INVALID_RESULT);
	}

	private WorkflowReviewerInput input() {
		return new WorkflowReviewerInput("wf-1", "https://github.com/acme/demo.git", "acme", "demo", 42,
				workspace.toString(), "build ok");
	}
}
