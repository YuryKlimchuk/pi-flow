package com.hydroyura.agents.piflow.workspace.domain;

import java.nio.file.Path;
import java.util.List;

/**
 * Внутренний driven-порт локального Git (реализуется JGit-адаптером).
 */
public interface LocalGit {

	void cloneRepository(String repositoryUrl, Path directory, String targetBranch);

	void updateToRemote(Path directory, String targetBranch);

	boolean isRepository(Path directory);

	boolean isClean(Path directory);

	String currentBranch(Path directory);

	boolean branchExists(Path directory, String branch);

	void createBranch(Path directory, String branch, String fromBranch);

	void checkout(Path directory, String branch);

	String commit(Path directory, String message, List<String> relativePaths);
}
