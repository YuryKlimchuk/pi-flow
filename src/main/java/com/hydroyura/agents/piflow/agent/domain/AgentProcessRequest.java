package com.hydroyura.agents.piflow.agent.domain;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Запрос на исполнение одного запуска (внутренняя модель).
 *
 * @param runId уникальный идентификатор запуска
 * @param role идентификатор роли
 * @param instruction инструкция (задача)
 * @param inputData входные данные
 * @param context контекст запуска
 * @param workingDirectory рабочая директория
 * @param parameters параметры запуска
 * @param timeout ограничение времени; {@code null} — использовать default адаптера
 */
public record AgentProcessRequest(
		String runId,
		String role,
		String instruction,
		String inputData,
		String context,
		Path workingDirectory,
		Map<String, String> parameters,
		Duration timeout) {

	public AgentProcessRequest {
		parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
	}
}
