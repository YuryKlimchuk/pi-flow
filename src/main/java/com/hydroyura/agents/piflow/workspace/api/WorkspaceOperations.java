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
}
