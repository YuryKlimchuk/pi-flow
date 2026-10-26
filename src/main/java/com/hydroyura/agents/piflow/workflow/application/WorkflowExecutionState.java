package com.hydroyura.agents.piflow.workflow.application;

/**
 * Актуальное состояние единого длительного исполнения Workflow.
 *
 * @param stage текущий этап
 * @param status статус workflow
 * @param workspacePath путь к подготовленному workspace
 * @param branchName предложенное имя ветки
 * @param failureReason причина ошибки
 */
public record WorkflowExecutionState(
		String stage,
		String status,
		String workspacePath,
		String branchName,
		String failureReason) {
}
