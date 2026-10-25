package com.hydroyura.agents.piflow.github.infrastructure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Typed-конфигурация подключения к GitHub API.
 *
 * @param baseUrl базовый URL GitHub API
 * @param token токен доступа (секрет, задаётся через внешнюю конфигурацию)
 * @param defaultPageSize размер страницы по умолчанию
 * @param connectTimeout тайм-аут установления соединения
 * @param readTimeout тайм-аут чтения ответа
 */
@ConfigurationProperties(prefix = "github.api")
public record GithubApiProperties(
		@DefaultValue("https://api.github.com") String baseUrl,
		String token,
		@DefaultValue("30") int defaultPageSize,
		@DefaultValue("5s") Duration connectTimeout,
		@DefaultValue("30s") Duration readTimeout) {
}
