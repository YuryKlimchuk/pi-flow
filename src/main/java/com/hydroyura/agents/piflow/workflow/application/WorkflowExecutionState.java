package com.hydroyura.agents.piflow.workflow.application;

/**
 * Актуальное состояние единого длительного исполнения Workflow.
 *
 * @param stage текущий этап
 * @param status статус workflow
 * @param workspacePath путь к подготовленному workspace
 * @param branchName предложенное имя ветки
 * @param failureReason причина ошибки
 * @param implementationRunId идентификатор запуска Developer
 * @param implementationDiagnostics диагностика сборки/тестов реализации
 * @param reviewRunId идентификатор запуска Reviewer
 * @param reviewText текст результата ревью
 * @param commitId идентификатор итогового коммита
 * @param branchUrl URL опубликованной ветки
 * @param prNumber номер созданного pull request
 * @param prUrl URL созданного pull request
 */
public record WorkflowExecutionState(
		String stage,
		String status,
		String workspacePath,
		String branchName,
		String failureReason,
		String implementationRunId,
		String implementationDiagnostics,
		String reviewRunId,
		String reviewText,
		String commitId,
		String branchUrl,
		long prNumber,
		String prUrl) {

	/** Компактный конструктор для сценариев без данных реализации/ревью/финализации. */
	public WorkflowExecutionState(String stage, String status, String workspacePath, String branchName, String failureReason) {
		this(stage, status, workspacePath, branchName, failureReason, null, null, null, null, null, null, 0, null);
	}
}
