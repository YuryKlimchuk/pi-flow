package com.hydroyura.agents.piflow.github.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class GithubApiPropertiesTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(EnableProperties.class);

	@Test
	void whenPropertiesMissing_shouldUseDefaults() {
		runner.run(context -> {
			assertThat(context).hasSingleBean(GithubApiProperties.class);
			GithubApiProperties properties = context.getBean(GithubApiProperties.class);

			assertThat(properties.baseUrl()).isEqualTo("https://api.github.com");
			assertThat(properties.defaultPageSize()).isEqualTo(30);
			assertThat(properties.token()).isNull();
		});
	}

	@Test
	void whenPropertiesSet_shouldBindValues() {
		runner.withPropertyValues(
				"github.api.base-url=http://localhost:8080",
				"github.api.token=secret",
				"github.api.connect-timeout=3s")
				.run(context -> {
					GithubApiProperties properties = context.getBean(GithubApiProperties.class);

					assertThat(properties.baseUrl()).isEqualTo("http://localhost:8080");
					assertThat(properties.token()).isEqualTo("secret");
					assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(GithubApiProperties.class)
	static class EnableProperties {
	}
}
