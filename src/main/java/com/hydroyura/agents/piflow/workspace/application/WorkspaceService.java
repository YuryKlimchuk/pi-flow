package com.hydroyura.agents.piflow.workspace.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.hydroyura.agents.piflow.workspace.api.WorkspaceArtifactResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceException;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspacePreparationRequest;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceState;
import com.hydroyura.agents.piflow.workspace.domain.LocalGit;
import com.hydroyura.agents.piflow.workspace.domain.WorkspaceGitException;
import com.hydroyura.agents.piflow.workspace.domain.WorkspaceLayout;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Реализует публичный порт подготовки локального рабочего пространства.
 */
@Service
@RequiredArgsConstructor
public class WorkspaceService implements WorkspaceOperations {

	private final LocalGit localGit;
	private final WorkspaceLayout layout;

	@Override
	public WorkspaceResult prepare(WorkspacePreparationRequest request) {
		request.validate();
		validateBaseDirectory();
		String baseBranch = localGit.resolveBaseBranch(request.repositoryUrl(), request.targetBranch());

		Path directory = layout.repositoryDirectory(request.repositoryUrl());
		try {
			if (Files.isDirectory(directory)) {
				prepareExisting(request, directory, baseBranch);
			} else {
				prepareNew(request, directory, baseBranch);
			}

			String branch = workingBranch(request.workflowId());
			if (!localGit.branchExists(directory, branch)) {
				localGit.createBranch(directory, branch, baseBranch);
			}
			localGit.checkout(directory, branch);

			WorkspaceState state = localGit.isClean(directory) ? WorkspaceState.CLEAN : WorkspaceState.DIRTY;
			return new WorkspaceResult(directory, request.repositoryUrl(), branch, state);
		} catch (WorkspaceGitException e) {
			throw WorkspaceErrors.map(e);
		}
	}

	@Override
	public WorkspaceArtifactResult readAnalysisArtifact(String workspacePath) {
		return readArtifact(workspacePath, ".pi-flow/analysis/analyst.md");
	}

	@Override
	public WorkspaceArtifactResult readDesignArtifact(String workspacePath) {
		WorkspaceArtifactResult result = readArtifact(workspacePath, ".pi-flow/design/architect.md");
		validateProposal(result.text());
		return result;
	}

	private void validateProposal(String text) {
		for (String heading : java.util.List.of("## Why", "## What Changes", "## Capabilities", "## Impact", "## Non-goals")) {
			if (!text.contains(heading)) {
				throw WorkspaceException.result("design proposal is missing required section: " + heading);
			}
		}
	}

	private WorkspaceArtifactResult readArtifact(String workspacePath, String relativePath) {
		if (workspacePath == null || workspacePath.isBlank()) {
			throw WorkspaceException.validation("workspace path must not be blank");
		}
		Path root = Path.of(workspacePath).normalize();
		Path artifact = root.resolve(relativePath).normalize();
		if (!artifact.startsWith(root)) {
			throw WorkspaceException.validation("artifact path escapes workspace root");
		}
		if (!Files.isRegularFile(artifact) || !Files.isReadable(artifact)) {
			throw WorkspaceException.result("artifact missing or not readable: " + artifact);
		}
		try {
			String text = Files.readString(artifact);
			if (text == null || text.isBlank()) {
				throw WorkspaceException.result("artifact is empty: " + artifact);
			}
			return new WorkspaceArtifactResult(text, artifact.toString(), Files.size(artifact));
		} catch (IOException e) {
			throw WorkspaceException.filesystem("cannot read artifact: " + artifact, e);
		}
	}

	private void prepareExisting(WorkspacePreparationRequest request, Path directory, String baseBranch) {
		if (!localGit.isRepository(directory)) {
			throw WorkspaceException.corrupt("directory is not a valid git repository: " + directory);
		}
		if (!localGit.isClean(directory)) {
			throw WorkspaceException.dirty("working copy has uncommitted changes: " + directory);
		}
		localGit.updateToRemote(directory, baseBranch);
	}

	private void prepareNew(WorkspacePreparationRequest request, Path directory, String baseBranch) {
		try {
			Files.createDirectories(directory.getParent());
		} catch (IOException e) {
			throw WorkspaceException.filesystem("cannot create base directory: " + directory.getParent(), e);
		}
		localGit.cloneRepository(request.repositoryUrl(), directory, baseBranch);
	}

	private void validateBaseDirectory() {
		Path base = layout.baseDirectory();
		try {
			Files.createDirectories(base);
		} catch (IOException e) {
			throw WorkspaceException.filesystem("cannot create base directory: " + base, e);
		}
		if (!Files.isDirectory(base) || !Files.isWritable(base)) {
			throw WorkspaceException.filesystem("base directory is not an accessible directory: " + base, null);
		}
	}

	private String workingBranch(String workflowId) {
		String branch = "pi-flow/" + workflowId;
		if (workflowId.chars().anyMatch(c -> Character.isWhitespace(c) || "~^:?*[\\".indexOf(c) >= 0)
				|| workflowId.contains("..") || workflowId.endsWith(".")) {
			throw WorkspaceException.validation("workflowId is not valid for branch naming: " + workflowId);
		}
		return branch;
	}
}
