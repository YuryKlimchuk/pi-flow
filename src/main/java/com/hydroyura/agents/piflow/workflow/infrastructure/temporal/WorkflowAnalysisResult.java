package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Результат activity анализа проекта.
 *
 * @param workspacePath путь к подготовленному workspace
 * @param branchName предложенное Analyst имя ветки
 * @param runId идентификатор запуска агента
 * @param artifactSize размер артефакта в байтах
 */
public record WorkflowAnalysisResult(
		String workspacePath,
		String branchName,
		String runId,
		long artifactSize) {
}
