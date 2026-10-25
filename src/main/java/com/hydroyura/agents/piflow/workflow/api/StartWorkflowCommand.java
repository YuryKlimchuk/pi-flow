package com.hydroyura.agents.piflow.workflow.api;

/**
 * Публичная команда запуска Workflow.
 *
 * @param workflowId идентификатор workflow
 * @param repositoryUrl адрес репозитория
 * @param issueNumber номер задачи GitHub
 */
public record StartWorkflowCommand(
		String workflowId,
		String repositoryUrl,
		long issueNumber) {

	/**
	 * Проверяет обязательные поля команды до запуска.
	 *
	 * @throws WorkflowException если обязательное поле отсутствует или некорректно
	 */
	public void validate() {
		if (workflowId == null || workflowId.isBlank()) {
			throw WorkflowException.validation("workflowId must not be blank");
		}
		if (repositoryUrl == null || repositoryUrl.isBlank()) {
			throw WorkflowException.validation("repositoryUrl must not be blank");
		}
		if (issueNumber <= 0) {
			throw WorkflowException.validation("issueNumber must be positive");
		}
	}
}
