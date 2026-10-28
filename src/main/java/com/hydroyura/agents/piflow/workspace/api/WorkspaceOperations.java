package com.hydroyura.agents.piflow.workspace.api;

/**
 * Публичный порт подготовки локального рабочего пространства.
 */
public interface WorkspaceOperations {

	/**
	 * Подготавливает локальную копию репозитория и рабочую ветку.
	 *
	 * @param request параметры подготовки
	 * @return результат с путём, репозиторием, веткой и состоянием
	 */
	WorkspaceResult prepare(WorkspacePreparationRequest request);

	/**
	 * Читает ожидаемый артефакт анализа {@code .pi-flow/analysis/analyst.md}
	 * внутри подготовленного workspace.
	 *
	 * @param workspacePath путь к подготовленному workspace
	 * @return текст и метаданные артефакта
	 */
	WorkspaceArtifactResult readAnalysisArtifact(String workspacePath);

	/**
	 * Читает ожидаемый артефакт проектирования {@code .pi-flow/design/architect.md}
	 * внутри подготовленного workspace.
	 *
	 * @param workspacePath путь к подготовленному workspace
	 * @return текст и метаданные артефакта
	 */
	WorkspaceArtifactResult readDesignArtifact(String workspacePath);

	/**
	 * Создаёт (или переключается на) итоговую ветку в подготовленном workspace.
	 *
	 * @param command параметры ветки
	 * @return результат с путём и именем ветки
	 */
	WorkspaceBranchResult createBranch(WorkspaceBranchCommand command);

	/**
	 * Возвращает ветку, из которой следует строить рабочую копию: предпочтительную,
	 * если она существует в удалённом репозитории, иначе {@code main}, иначе саму
	 * предпочтительную ветку.
	 *
	 * @param repositoryUrl адрес репозитория
	 * @param preferredBranch предпочтительная базовая ветка (например, {@code master})
	 * @return фактическая базовая ветка
	 */
	String resolveBaseBranch(String repositoryUrl, String preferredBranch);
}
