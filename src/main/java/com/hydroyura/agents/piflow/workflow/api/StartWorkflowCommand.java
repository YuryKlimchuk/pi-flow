package com.hydroyura.agents.piflow.workflow.api;

/**
 * Публичная команда запуска Workflow по конкретной задаче (Issue).
 *
 * @param workflowId идентификатор workflow
 * @param owner владелец репозитория
 * @param repository имя репозитория
 * @param issueNumber номер задачи GitHub
 */
public record StartWorkflowCommand(
		String workflowId,
		String owner,
		String repository,
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
		if (owner == null || owner.isBlank()) {
			throw WorkflowException.validation("owner must not be blank");
		}
		if (repository == null || repository.isBlank()) {
			throw WorkflowException.validation("repository must not be blank");
		}
		if (issueNumber <= 0) {
			throw WorkflowException.validation("issueNumber must be positive");
		}
	}
}
