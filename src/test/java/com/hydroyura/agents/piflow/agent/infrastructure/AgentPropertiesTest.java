package com.hydroyura.agents.piflow.agent.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AgentPropertiesTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(EnableProperties.class);

	@Test
	void whenPropertiesMissing_shouldUseDefaults() {
		runner.run(context -> {
			AgentProperties properties = context.getBean(AgentProperties.class);

			assertThat(properties.executable()).isEqualTo("pi");
			assertThat(properties.baseArgs()).isEmpty();
			assertThat(properties.allowedEnvironmentVariables()).isEmpty();
			assertThat(properties.defaultTimeout()).isEqualTo(Duration.ofMinutes(10));
			assertThat(properties.profiles()).isEmpty();
		});
	}

	@Test
	void whenPropertiesSet_shouldBindValues() {
		runner.withPropertyValues(
				"agent.executable=/usr/local/bin/pi",
				"agent.default-timeout=5m",
				"agent.base-args[0]=--verbose",
				"agent.allowed-environment-variables[0]=FOO",
				"agent.profiles.ANALYST=profile text")
				.run(context -> {
					AgentProperties properties = context.getBean(AgentProperties.class);

					assertThat(properties.executable()).isEqualTo("/usr/local/bin/pi");
					assertThat(properties.defaultTimeout()).isEqualTo(Duration.ofMinutes(5));
					assertThat(properties.baseArgs()).containsExactly("--verbose");
					assertThat(properties.allowedEnvironmentVariables()).containsExactly("FOO");
					assertThat(properties.profiles()).containsEntry("ANALYST", "profile text");
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(AgentProperties.class)
	static class EnableProperties {
	}
}
