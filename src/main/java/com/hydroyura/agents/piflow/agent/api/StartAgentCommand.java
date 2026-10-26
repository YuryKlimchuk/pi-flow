package com.hydroyura.agents.piflow.agent.api;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Входная модель запуска задачи агента.
 *
 * @param role роль/профиль запускаемого агента
 * @param instruction инструкция (задача) для агента
 * @param inputData входные данные
 * @param context дополнительный контекст запуска
 * @param workingDirectory рабочая директория проекта
 * @param parameters параметры запуска
 * @param timeout ограничение времени выполнения; {@code null} — использовать default из конфигурации
 * @param feedback необязательные замечания пользователя для агента
 */
public record StartAgentCommand(
		AgentRole role,
		String instruction,
		String inputData,
		String context,
		Path workingDirectory,
		Map<String, String> parameters,
		Duration timeout,
		String feedback) {

	public StartAgentCommand {
		parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
	}

	/**
	 * Проверяет обязательные поля команды до запуска.
	 *
	 * @throws AgentException если обязательное поле отсутствует или некорректно
	 */
	public void validate() {
		if (role == null) {
			throw AgentException.preparation("role must not be null");
		}
		if (instruction == null || instruction.isBlank()) {
			throw AgentException.preparation("instruction must not be blank");
		}
		if (workingDirectory == null) {
			throw AgentException.preparation("workingDirectory must not be null");
		}
		if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
			throw AgentException.preparation("timeout must be positive");
		}
	}
}
