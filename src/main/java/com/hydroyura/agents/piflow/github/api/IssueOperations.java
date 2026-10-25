package com.hydroyura.agents.piflow.github.api;

/**
 * Публичный порт получения задач (Issues) GitHub.
 */
public interface IssueOperations {

	/**
	 * Возвращает страницу задач репозитория.
	 *
	 * @param owner владелец репозитория
	 * @param repository имя репозитория
	 * @param filter фильтр задач
	 * @param page параметры страницы
	 * @return страница задач
	 */
	IssuePage listIssues(String owner, String repository, IssueFilter filter, PageRequest page);

	/**
	 * Возвращает детали задачи по её номеру.
	 *
	 * @param owner владелец репозитория
	 * @param repository имя репозитория
	 * @param number номер задачи
	 * @return детали задачи
	 */
	IssueDetails getIssue(String owner, String repository, long number);
}
