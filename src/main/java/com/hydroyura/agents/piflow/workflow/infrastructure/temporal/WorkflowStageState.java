package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Компактный снапшот состояния длинного workflow для query.
 *
 * @param stage текущий этап
 * @param status статус workflow
 * @param workspacePath путь к подготовленному workspace
 * @param branchName предложенное имя ветки
 * @param analysisArtifactPath путь к артефакту анализа
 * @param designArtifactPath путь к артефакту проектирования
 * @param failureReason причина ошибки
 */
public record WorkflowStageState(
		String stage,
		String status,
		String workspacePath,
		String branchName,
		String analysisArtifactPath,
		String designArtifactPath,
		String failureReason) {
}
