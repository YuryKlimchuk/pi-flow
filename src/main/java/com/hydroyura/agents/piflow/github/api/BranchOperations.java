package com.hydroyura.agents.piflow.github.api;

/**
 * Публичный порт публикации подготовленной ветки в GitHub.
 */
public interface BranchOperations {

	/**
	 * Публикует уже подготовленную ветку и возвращает её удалённый ref и URL.
	 *
	 * @param owner владелец репозитория
	 * @param repository имя репозитория
	 * @param branch имя подготовленной ветки
	 * @return результат публикации
	 */
	BranchPublication publishBranch(String owner, String repository, String branch);
}
