package com.hydroyura.agents.piflow.workflow.api;

/**
 * Публичная команда публикации изменений (commit + push).
 *
 * @param workflowId идентификатор workflow
 */
public record PushWorkflowCommand(String workflowId) {

	public void validate() {
		if (workflowId == null || workflowId.isBlank()) {
			throw WorkflowException.validation("workflowId must not be blank");
		}
	}
}
