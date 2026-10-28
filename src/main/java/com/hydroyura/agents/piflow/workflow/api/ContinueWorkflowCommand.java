package com.hydroyura.agents.piflow.workflow.api;

/**
 * Публичная команда продолжения Workflow на текущем ручном gate
 * (анализ, проектирование или ревью).
 *
 * @param workflowId идентификатор workflow
 */
public record ContinueWorkflowCommand(String workflowId) {

	/**
	 * Проверяет обязательные поля команды до продолжения.
	 *
	 * @throws WorkflowException если обязательное поле отсутствует или некорректно
	 */
	public void validate() {
		if (workflowId == null || workflowId.isBlank()) {
			throw WorkflowException.validation("workflowId must not be blank");
		}
	}
}
