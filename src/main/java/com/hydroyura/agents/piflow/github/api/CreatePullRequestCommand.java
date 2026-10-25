package com.hydroyura.agents.piflow.github.api;

/**
 * Параметры создания pull request.
 *
 * @param owner владелец репозитория
 * @param repository имя репозитория
 * @param base базовая ветка
 * @param head head-ветка (подготовленная ветка)
 * @param title заголовок pull request
 * @param description описание pull request
 */
public record CreatePullRequestCommand(
		String owner,
		String repository,
		String base,
		String head,
		String title,
		String description) {

	/**
	 * Проверяет обязательные поля команды до обращения к GitHub.
	 *
	 * @throws GithubException если обязательное поле отсутствует или пустое
	 */
	public void validate() {
		requireText(owner, "owner");
		requireText(repository, "repository");
		requireText(base, "base");
		requireText(head, "head");
		requireText(title, "title");
	}

	private static void requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw GithubException.validation("createPullRequest", 0, field + " must not be blank");
		}
	}
}
