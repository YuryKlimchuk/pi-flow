package com.hydroyura.agents.piflow.workflow.application;

/**
 * Порт запуска длительного исполнения Workflow.
 *
 * <p>Скрывает конкретную реализацию оркестрации (например, Temporal) от
 * application-сценария и домена.</p>
 */
public interface WorkflowExecutionPort {

	/**
	 * Запускает длительное исполнение Workflow.
	 *
	 * @param command команда исполнения
	 * @return идентификатор исполнения
	 */
	WorkflowExecutionResult start(WorkflowExecutionCommand command);

	/**
	 * Возвращает актуальное состояние длительного исполнения по идентификатору.
	 *
	 * @param executionId идентификатор исполнения
	 * @return фаза исполнения и результат/ошибку
	 */
	WorkflowExecutionState getState(String executionId);
}
