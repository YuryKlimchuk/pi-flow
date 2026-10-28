package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import java.util.List;

import com.hydroyura.agents.piflow.github.api.BranchOperations;
import com.hydroyura.agents.piflow.github.api.BranchPublication;
import com.hydroyura.agents.piflow.github.api.CreatePullRequestCommand;
import com.hydroyura.agents.piflow.github.api.PullRequestOperations;
import com.hydroyura.agents.piflow.github.api.PullRequestResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceBranchCommand;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceCommitCommand;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceCommitResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceGitOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspacePushCommand;
import lombok.extern.slf4j.Slf4j;

/**
 * Реализация activity финализации: делегирует локальные Git-операции модулю
 * {@code workspace}, а публикацию ветки и создание PR — модулю {@code github}.
 *
 * <p>Не содержит прямых вызовов JGit, GitHub HTTP-клиента или subprocess.</p>
 */
@Slf4j
public class WorkflowFinalizationActivitiesImpl implements WorkflowFinalizationActivities {

	private final WorkspaceOperations workspaceOperations;
	private final WorkspaceGitOperations workspaceGitOperations;
	private final BranchOperations branchOperations;
	private final PullRequestOperations pullRequestOperations;

	public WorkflowFinalizationActivitiesImpl(
			WorkspaceOperations workspaceOperations,
			WorkspaceGitOperations workspaceGitOperations,
			BranchOperations branchOperations,
			PullRequestOperations pullRequestOperations) {
		this.workspaceOperations = workspaceOperations;
		this.workspaceGitOperations = workspaceGitOperations;
		this.branchOperations = branchOperations;
		this.pullRequestOperations = pullRequestOperations;
	}

	@Override
	public WorkflowFinalizationResult finalize(WorkflowFinalizationInput input) {
		validateInput(input);
		log.info("finalization started: workflowId={} branch={} base={}",
				input.workflowId(), input.branchName(), input.baseBranch());
		try {
			WorkspaceCommitResult commit = workspaceGitOperations.commit(new WorkspaceCommitCommand(
					input.workflowId(), input.repositoryUrl(), "pi-flow: implement issue #" + input.issueNumber(),
					List.of()));
			log.info("finalization commit done: workflowId={} commit={}", input.workflowId(), commit.commitId());
			workspaceOperations.createBranch(new WorkspaceBranchCommand(
					input.workflowId(), input.repositoryUrl(), input.branchName()));
			workspaceGitOperations.push(new WorkspacePushCommand(
					input.workflowId(), input.repositoryUrl(), input.branchName()));
			log.info("finalization push done: workflowId={} branch={}", input.workflowId(), input.branchName());
			BranchPublication publication = branchOperations.publishBranch(
					input.owner(), input.repository(), input.branchName());
			PullRequestResult pr = pullRequestOperations.createPullRequest(new CreatePullRequestCommand(
					input.owner(), input.repository(), input.baseBranch(), input.branchName(),
					"pi-flow: issue #" + input.issueNumber(),
					"Автоматическая реализация задачи #" + input.issueNumber() + " (pi-flow)."));
			log.info("finalization PR created: workflowId={} pr={} url={}", input.workflowId(), pr.number(), pr.url());
			return new WorkflowFinalizationResult(commit.commitId(), publication.url(), pr.number(), pr.url());
		} catch (RuntimeException e) {
			log.error("finalization failed: workflowId={} branch={} base={}",
					input.workflowId(), input.branchName(), input.baseBranch(), e);
			throw WorkflowActivityException.orchestrationFailed("finalization failed: " + e.getMessage(), e);
		}
	}

	private void validateInput(WorkflowFinalizationInput input) {
		if (input == null || input.workflowId() == null || input.workflowId().isBlank()
				|| input.repositoryUrl() == null || input.repositoryUrl().isBlank()
				|| input.branchName() == null || input.branchName().isBlank()) {
			throw WorkflowActivityException.invalidResult("finalization input is incomplete");
		}
	}
}
