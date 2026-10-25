package com.hydroyura.agents.piflow.github.api;

/**
 * Публичный порт получения репозиториев GitHub.
 */
public interface RepositoryOperations {

	/**
	 * Возвращает страницу доступных репозиториев.
	 *
	 * @param page параметры страницы
	 * @return страница репозиториев
	 */
	RepositoryPage listRepositories(PageRequest page);
}
