package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Результат activity анализа проекта.
 *
 * @param artifactPath путь к сохранённому артефакту анализа
 * @param runId идентификатор запуска агента
 * @param artifactSize размер артефакта в байтах
 */
public record WorkflowAnalysisResult(
		String artifactPath,
		String runId,
		long artifactSize) {
}
