package com.hydroyura.agents.piflow.agent.api;

import java.util.Map;

/**
 * Выходная модель технического результата запуска агента.
 *
 * @param runId уникальный идентификатор запуска
 * @param role роль запуска
 * @param status технический статус завершения
 * @param stdout текстовый ответ из stdout
 * @param stderr диагностический вывод stderr
 * @param exitCode код завершения процесса, если доступен
 * @param metadata технические метаданные запуска
 */
public record AgentRunResult(
		String runId,
		AgentRole role,
		AgentRunStatus status,
		String stdout,
		String stderr,
		Integer exitCode,
		Map<String, String> metadata) {
}
