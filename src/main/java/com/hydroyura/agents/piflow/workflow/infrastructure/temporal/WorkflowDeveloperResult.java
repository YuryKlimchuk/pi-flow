package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Результат activity реализации решения Developer.
 *
 * @param workspacePath путь к подготовленному workspace
 * @param runId идентификатор запуска агента
 * @param success признак успешной сборки и тестов
 * @param diagnostics диагностические сведения сборки/тестов (stdout/stderr)
 */
public record WorkflowDeveloperResult(
		String workspacePath,
		String runId,
		boolean success,
		String diagnostics) {
}
