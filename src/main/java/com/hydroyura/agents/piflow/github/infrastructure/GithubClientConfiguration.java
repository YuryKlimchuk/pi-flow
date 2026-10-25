package com.hydroyura.agents.piflow.github.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Собирает {@link RestClient} для GitHub API из typed-конфигурации.
 *
 * <p>Использует автоконфигурируемый Spring Boot {@link RestClient.Builder},
 * чтобы к клиенту применялись стандартные конвертеры сообщений (включая JSON).</p>
 */
@Configuration
public class GithubClientConfiguration {

	@Bean
	RestClient githubRestClient(RestClient.Builder builder, GithubApiProperties properties) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(properties.connectTimeout());
		factory.setReadTimeout(properties.readTimeout());

		return builder
				.baseUrl(properties.baseUrl())
				.requestFactory(factory)
				.build();
	}
}
