package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import com.hydroyura.agents.piflow.github.api.BranchOperations;
import com.hydroyura.agents.piflow.github.api.BranchPublication;
import com.hydroyura.agents.piflow.github.api.CreatePullRequestCommand;
import com.hydroyura.agents.piflow.github.api.PullRequestOperations;
import com.hydroyura.agents.piflow.github.api.PullRequestResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceBranchCommand;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceBranchResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceCommitCommand;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceCommitResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceGitOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspacePushCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit-тест activity финализации: порядок операций и делегирование в публичные контракты.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowFinalizationActivitiesImplTest {

	@Mock
	private WorkspaceOperations workspaceOperations;

	@Mock
	private WorkspaceGitOperations workspaceGitOperations;

	@Mock
	private BranchOperations branchOperations;

	@Mock
	private PullRequestOperations pullRequestOperations;

	private WorkflowFinalizationActivitiesImpl activity;

	@BeforeEach
	void setUp() {
		activity = new WorkflowFinalizationActivitiesImpl(
				workspaceOperations, workspaceGitOperations, branchOperations, pullRequestOperations);
	}

	@Test
	void finalize_shouldDelegateInOrderAndReturnPr() {
		when(workspaceGitOperations.commit(any())).thenReturn(
				new WorkspaceCommitResult(Path.of("/ws"), "feature/issue-42", "commit-1"));
		when(workspaceOperations.createBranch(any())).thenReturn(
				new WorkspaceBranchResult(Path.of("/ws"), "feature/issue-42"));
		when(branchOperations.publishBranch("acme", "demo", "feature/issue-42")).thenReturn(
				new BranchPublication("refs/heads/feature/issue-42", "https://github.com/acme/demo/tree/feature/issue-42"));
		when(pullRequestOperations.createPullRequest(any())).thenReturn(
				new PullRequestResult(7, "https://github.com/acme/demo/pull/7"));

		WorkflowFinalizationResult result = activity.finalize(input());

		assertThat(result.commitId()).isEqualTo("commit-1");
		assertThat(result.prNumber()).isEqualTo(7);
		assertThat(result.prUrl()).isEqualTo("https://github.com/acme/demo/pull/7");

		InOrder order = inOrder(workspaceGitOperations, workspaceOperations, branchOperations, pullRequestOperations);
		order.verify(workspaceGitOperations).commit(any(WorkspaceCommitCommand.class));
		order.verify(workspaceOperations).createBranch(any(WorkspaceBranchCommand.class));
		order.verify(workspaceGitOperations).push(any(WorkspacePushCommand.class));
		order.verify(branchOperations).publishBranch("acme", "demo", "feature/issue-42");
		order.verify(pullRequestOperations).createPullRequest(any(CreatePullRequestCommand.class));
	}

	@Test
	void finalize_whenPrFails_shouldThrowOrchestrationFailed() {
		when(workspaceGitOperations.commit(any())).thenReturn(
				new WorkspaceCommitResult(Path.of("/ws"), "feature/issue-42", "commit-1"));
		when(workspaceOperations.createBranch(any())).thenReturn(
				new WorkspaceBranchResult(Path.of("/ws"), "feature/issue-42"));
		when(branchOperations.publishBranch("acme", "demo", "feature/issue-42")).thenReturn(
				new BranchPublication("refs/heads/feature/issue-42", "https://github.com/acme/demo/tree/feature/issue-42"));
		when(pullRequestOperations.createPullRequest(any())).thenThrow(new RuntimeException("github down"));

		WorkflowActivityException error = catchThrowableOfType(() -> activity.finalize(input()),
				WorkflowActivityException.class);

		assertThat(error.reason()).isEqualTo(WorkflowActivityException.Reason.ORCHESTRATION_FAILED);
		verify(pullRequestOperations).createPullRequest(any());
	}

	@Test
	void finalize_whenInputIncomplete_shouldThrowInvalidResult() {
		WorkflowActivityException error = catchThrowableOfType(
				() -> activity.finalize(new WorkflowFinalizationInput(null, "url", "acme", "demo", 42, "b", "master")),
				WorkflowActivityException.class);

		assertThat(error.reason()).isEqualTo(WorkflowActivityException.Reason.INVALID_RESULT);
	}

	private WorkflowFinalizationInput input() {
		return new WorkflowFinalizationInput("wf-1", "https://github.com/acme/demo.git", "acme", "demo", 42,
				"feature/issue-42", "master");
	}
}
