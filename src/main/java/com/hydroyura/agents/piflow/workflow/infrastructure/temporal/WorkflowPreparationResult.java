package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Результат подготовки workspace в activity.
 *
 * @param path путь к локальной копии репозитория
 * @param branch активная рабочая ветка
 * @param lockKey стабильный ключ блокировки репозитория
 * @param lockToken токен владельца блокировки
 * @param baseBranch фактическая базовая ветка репозитория
 */
public record WorkflowPreparationResult(String path, String branch, String lockKey, String lockToken, String baseBranch) {

	public WorkflowPreparationResult(String path, String branch) {
		this(path, branch, null, null, null);
	}
}
