package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Входная модель activity реализации решения Developer.
 *
 * @param workflowId идентификатор workflow
 * @param repositoryUrl нормализованный URL репозитория
 * @param owner владелец репозитория
 * @param repository имя репозитория
 * @param issueNumber номер задачи GitHub
 * @param workspacePath путь к подготовленному workspace
 * @param designText утверждённый design-артефакт Architect
 * @param feedback замечания пользователя для повторной реализации (опционально)
 */
public record WorkflowDeveloperInput(
		String workflowId,
		String repositoryUrl,
		String owner,
		String repository,
		long issueNumber,
		String workspacePath,
		String designText,
		String feedback) {
}
