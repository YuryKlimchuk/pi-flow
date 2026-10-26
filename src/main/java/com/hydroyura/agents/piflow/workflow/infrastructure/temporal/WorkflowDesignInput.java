package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Входная модель activity проектирования (Architect).
 *
 * @param workflowId идентификатор workflow
 * @param repositoryUrl нормализованный URL репозитория
 * @param owner владелец репозитория
 * @param repository имя репозитория
 * @param issueNumber номер задачи GitHub
 * @param workspacePath путь к подготовленному workspace
 * @param analysisText текст результата анализа Analyst
 */
public record WorkflowDesignInput(
		String workflowId,
		String repositoryUrl,
		String owner,
		String repository,
		long issueNumber,
		String workspacePath,
		String analysisText) {
}
