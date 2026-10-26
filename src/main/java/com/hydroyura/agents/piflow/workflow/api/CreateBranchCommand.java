package com.hydroyura.agents.piflow.workflow.api;

/**
 * Публичная команда создания итоговой ветки.
 *
 * @param workflowId идентификатор workflow
 */
public record CreateBranchCommand(String workflowId) {

	public void validate() {
		if (workflowId == null || workflowId.isBlank()) {
			throw WorkflowException.validation("workflowId must not be blank");
		}
	}
}
