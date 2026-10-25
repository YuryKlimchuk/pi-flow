package com.hydroyura.agents.piflow.github.api;

/**
 * Публичный порт создания pull request.
 */
public interface PullRequestOperations {

	/**
	 * Создаёт pull request либо возвращает уже существующий открытый PR.
	 *
	 * @param command параметры создания pull request
	 * @return номер и URL pull request
	 */
	PullRequestResult createPullRequest(CreatePullRequestCommand command);
}
