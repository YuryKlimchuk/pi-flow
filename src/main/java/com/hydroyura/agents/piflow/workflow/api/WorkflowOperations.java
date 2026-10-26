package com.hydroyura.agents.piflow.workflow.api;

import java.util.List;

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

	/**
	 * Продолжает Workflow после успешного анализа с предложенным именем ветки.
	 *
	 * @param command команда продолжения
	 * @return публичное состояние после перехода на следующий этап
	 */
	WorkflowState continueWorkflow(ContinueWorkflowCommand command);

	/**
	 * Перезапускает анализ Workflow с замечаниями пользователя.
	 *
	 * @param command команда повторного анализа
	 * @return публичное состояние после возврата к анализу
	 */
	WorkflowState rework(ReworkWorkflowCommand command);

	/**
	 * Возвращает список запущенных workflow.
	 *
	 * @return список компактных описаний workflow
	 */
	List<WorkflowSummary> listWorkflows();
}
