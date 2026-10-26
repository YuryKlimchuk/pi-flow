package com.hydroyura.agents.piflow.workflow.api;

/**
 * Публичная команда продолжения Workflow после успешного анализа.
 *
 * @param workflowId идентификатор workflow
 * @param branchName предложенное Analyst имя ветки
 */
public record ContinueWorkflowCommand(String workflowId, String branchName) {

	/**
	 * Проверяет обязательные поля команды до продолжения.
	 *
	 * @throws WorkflowException если обязательное поле отсутствует или некорректно
	 */
	public void validate() {
		if (workflowId == null || workflowId.isBlank()) {
			throw WorkflowException.validation("workflowId must not be blank");
		}
		if (branchName == null || branchName.isBlank()) {
			throw WorkflowException.validation("branchName must not be blank");
		}
	}
}
