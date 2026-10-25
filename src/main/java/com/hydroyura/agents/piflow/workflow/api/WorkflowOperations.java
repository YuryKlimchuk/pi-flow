package com.hydroyura.agents.piflow.workflow.api;

/**
 * Публичный порт запуска и чтения состояния Workflow.
 */
public interface WorkflowOperations {

	/**
	 * Запускает Workflow по команде.
	 *
	 * @param command команда запуска
	 * @return публичное состояние созданного Workflow
	 */
	WorkflowState start(StartWorkflowCommand command);

	/**
	 * Возвращает публичное состояние Workflow по идентификатору.
	 *
	 * @param workflowId идентификатор workflow
	 * @return публичное состояние
	 */
	WorkflowState getState(String workflowId);
}
