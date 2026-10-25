package com.hydroyura.agents.piflow.workspace.application;

import java.nio.file.Path;

import com.hydroyura.agents.piflow.workspace.api.WorkspaceCommitCommand;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceCommitResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceException;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceGitOperations;
import com.hydroyura.agents.piflow.workspace.domain.LocalGit;
import com.hydroyura.agents.piflow.workspace.domain.WorkspaceGitException;
import com.hydroyura.agents.piflow.workspace.domain.WorkspaceLayout;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Реализует публичный порт локальных Git-операций.
 */
@Service
@RequiredArgsConstructor
public class WorkspaceGitService implements WorkspaceGitOperations {

	private final LocalGit localGit;
	private final WorkspaceLayout layout;

	@Override
	public WorkspaceCommitResult commit(WorkspaceCommitCommand command) {
		command.validate();
		Path directory = layout.repositoryDirectory(command.repositoryUrl());

		try {
			if (!localGit.isRepository(directory)) {
				throw WorkspaceException.corrupt("directory is not a valid git repository: " + directory);
			}
			String branch = localGit.currentBranch(directory);
			String commitId = localGit.commit(directory, command.message(), command.relativePaths());
			return new WorkspaceCommitResult(directory, branch, commitId);
		} catch (WorkspaceGitException e) {
			throw WorkspaceErrors.map(e);
		}
	}
}
