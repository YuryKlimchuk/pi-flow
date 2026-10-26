package com.hydroyura.agents.piflow.workflow.api;

/**
 * Публичная команда повторного анализа с замечаниями пользователя.
 *
 * @param workflowId идентификатор workflow
 * @param feedback замечания пользователя для Analyst
 */
public record ReworkWorkflowCommand(String workflowId, String feedback) {

	/**
	 * Проверяет обязательные поля команды до повторного анализа.
	 *
	 * @throws WorkflowException если обязательное поле отсутствует или некорректно
	 */
	public void validate() {
		if (workflowId == null || workflowId.isBlank()) {
			throw WorkflowException.validation("workflowId must not be blank");
		}
		if (feedback == null || feedback.isBlank()) {
			throw WorkflowException.validation("feedback must not be blank");
		}
	}
}
