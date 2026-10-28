package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Входная модель activity независимого ревью Reviewer.
 *
 * @param workflowId идентификатор workflow
 * @param repositoryUrl нормализованный URL репозитория
 * @param owner владелец репозитория
 * @param repository имя репозитория
 * @param issueNumber номер задачи GitHub
 * @param workspacePath путь к подготовленному workspace
 * @param implementationDiagnostics диагностика сборки/тестов реализации
 */
public record WorkflowReviewerInput(
		String workflowId,
		String repositoryUrl,
		String owner,
		String repository,
		long issueNumber,
		String workspacePath,
		String implementationDiagnostics) {
}
