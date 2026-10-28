package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Компактный снапшот состояния длинного workflow для query.
 *
 * @param stage текущий этап
 * @param status статус workflow
 * @param workspacePath путь к подготовленному workspace
 * @param branchName предложенное имя ветки
 * @param implementationRunId идентификатор запуска Developer
 * @param implementationDiagnostics диагностика сборки/тестов реализации
 * @param reviewRunId идентификатор запуска Reviewer
 * @param reviewText текст результата ревью
 * @param commitId идентификатор итогового коммита
 * @param branchUrl URL опубликованной ветки
 * @param prNumber номер созданного pull request
 * @param prUrl URL созданного pull request
 * @param failureReason причина ошибки
 */
public record WorkflowStageState(
		String stage,
		String status,
		String workspacePath,
		String branchName,
		String implementationRunId,
		String implementationDiagnostics,
		String reviewRunId,
		String reviewText,
		String commitId,
		String branchUrl,
		long prNumber,
		String prUrl,
		String failureReason) {
}
