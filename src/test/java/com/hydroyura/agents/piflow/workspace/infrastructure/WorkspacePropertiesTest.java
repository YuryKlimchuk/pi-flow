package com.hydroyura.agents.piflow.workspace.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class WorkspacePropertiesTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(EnableProperties.class);

	@Test
	void whenPropertiesMissing_shouldUseDefaults() {
		runner.run(context -> {
			WorkspaceProperties properties = context.getBean(WorkspaceProperties.class);

			assertThat(properties.baseDirectory()).isNull();
			assertThat(properties.lockDirectory()).isNull();
			assertThat(properties.leaseDuration()).isEqualTo(Duration.ofMinutes(5));
		});
	}

	@Test
	void whenPropertiesSet_shouldBindValues() {
		runner.withPropertyValues(
				"workspace.base-directory=/tmp/ws",
				"workspace.lock-directory=/tmp/locks",
				"workspace.lease-duration=3m")
				.run(context -> {
					WorkspaceProperties properties = context.getBean(WorkspaceProperties.class);

					assertThat(properties.baseDirectory()).isEqualTo(Path.of("/tmp/ws"));
					assertThat(properties.lockDirectory()).isEqualTo(Path.of("/tmp/locks"));
					assertThat(properties.leaseDuration()).isEqualTo(Duration.ofMinutes(3));
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WorkspaceProperties.class)
	static class EnableProperties {
	}
}
