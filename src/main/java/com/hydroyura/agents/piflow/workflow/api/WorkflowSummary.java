package com.hydroyura.agents.piflow.workflow.api;

/**
 * Компактное описание запущенного workflow для списка.
 *
 * @param workflowId идентификатор workflow
 * @param repositoryUrl адрес репозитория
 * @param issueNumber номер задачи GitHub
 * @param status статус
 * @param stage текущий этап
 */
public record WorkflowSummary(
		String workflowId,
		String repositoryUrl,
		long issueNumber,
		String status,
		String stage) {
}
