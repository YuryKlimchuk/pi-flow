package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Входная модель activity финализации.
 *
 * @param workflowId идентификатор workflow
 * @param repositoryUrl нормализованный URL репозитория
 * @param owner владелец репозитория
 * @param repository имя репозитория
 * @param issueNumber номер задачи GitHub
 * @param branchName итоговое имя ветки (предложение Analyst)
 * @param baseBranch базовая ветка для pull request
 */
public record WorkflowFinalizationInput(
		String workflowId,
		String repositoryUrl,
		String owner,
		String repository,
		long issueNumber,
		String branchName,
		String baseBranch) {
}
