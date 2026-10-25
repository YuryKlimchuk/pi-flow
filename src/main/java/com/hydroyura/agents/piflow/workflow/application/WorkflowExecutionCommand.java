package com.hydroyura.agents.piflow.workflow.application;

/**
 * Команда запуска длительного исполнения Workflow через application-порт.
 *
 * @param workflowId идентификатор workflow
 * @param repositoryUrl нормализованный URL репозитория (clone URL)
 * @param owner владелец репозитория
 * @param repository имя репозитория
 * @param issueNumber номер задачи GitHub
 */
public record WorkflowExecutionCommand(
		String workflowId,
		String repositoryUrl,
		String owner,
		String repository,
		long issueNumber) {
}
