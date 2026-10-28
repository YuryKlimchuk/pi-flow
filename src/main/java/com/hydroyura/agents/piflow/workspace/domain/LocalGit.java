package com.hydroyura.agents.piflow.workspace.domain;

import java.nio.file.Path;
import java.util.List;

/**
 * Внутренний driven-порт локального Git (реализуется JGit-адаптером).
 */
public interface LocalGit {

	void cloneRepository(String repositoryUrl, Path directory, String targetBranch);

	void updateToRemote(Path directory, String targetBranch);

	/**
	 * Возвращает ветку, из которой следует строить рабочую копию: предпочтительную
	 * ветку, если она существует в удалённом репозитории, иначе {@code main}.
	 *
	 * @param repositoryUrl адрес репозитория
	 * @param preferredBranch предпочтительная ветка (например, {@code master})
	 * @return фактическая базовая ветка
	 */
	String resolveBaseBranch(String repositoryUrl, String preferredBranch);

	boolean isRepository(Path directory);

	boolean isClean(Path directory);

	String currentBranch(Path directory);

	boolean branchExists(Path directory, String branch);

	void createBranch(Path directory, String branch, String fromBranch);

	void checkout(Path directory, String branch);

	String commit(Path directory, String message, List<String> relativePaths);

	/**
	 * Публикует локальную ветку в удалённый репозиторий {@code origin}.
	 *
	 * @param directory локальная рабочая копия
	 * @param branch имя ветки для публикации
	 */
	void push(Path directory, String branch);
}
