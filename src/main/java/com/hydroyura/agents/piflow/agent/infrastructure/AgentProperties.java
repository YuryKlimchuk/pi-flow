package com.hydroyura.agents.piflow.agent.infrastructure;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Typed-конфигурация запуска Pi.
 *
 * @param executable путь к исполняемому файлу Pi
 * @param baseArgs базовые аргументы командной строки
 * @param allowedEnvironmentVariables имена переменных окружения, разрешённых к передаче
 * @param defaultTimeout тайм-аут выполнения по умолчанию
 * @param profiles профильные инструкции ролей (имя роли → инструкция)
 */
@ConfigurationProperties(prefix = "agent")
public record AgentProperties(
		@DefaultValue("pi") String executable,
		List<String> baseArgs,
		List<String> allowedEnvironmentVariables,
		@DefaultValue("10m") Duration defaultTimeout,
		Map<String, String> profiles) {

	public AgentProperties {
		baseArgs = baseArgs == null ? List.of() : List.copyOf(baseArgs);
		allowedEnvironmentVariables = allowedEnvironmentVariables == null ? List.of()
				: List.copyOf(allowedEnvironmentVariables);
		profiles = profiles == null ? Map.of() : Map.copyOf(profiles);
	}
}
