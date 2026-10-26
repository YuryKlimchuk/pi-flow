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
}
