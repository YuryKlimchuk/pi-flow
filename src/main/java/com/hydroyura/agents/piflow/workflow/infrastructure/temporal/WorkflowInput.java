package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Входная модель Temporal workflow.
 *
 * @param workflowId идентификатор workflow
 * @param repositoryUrl нормализованный URL репозитория (clone URL)
 * @param owner владелец репозитория
 * @param repository имя репозитория
 * @param issueNumber номер задачи GitHub
 * @param feedback замечания пользователя для Analyst (опционально)
 * @param analysisText результат анализа Analyst для Architect (опционально)
 */
public record WorkflowInput(
		String workflowId,
		String repositoryUrl,
		String owner,
		String repository,
		long issueNumber,
		String feedback,
		String analysisText) {
}
