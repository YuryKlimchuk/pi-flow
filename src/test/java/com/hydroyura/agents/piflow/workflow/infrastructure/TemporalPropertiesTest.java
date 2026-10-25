package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Проверяет связывание typed-конфигурации Temporal.
 */
class TemporalPropertiesTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(BindingConfiguration.class);

	@Test
	void bindsLocalDefaults() {
		runner.run(context -> {
			TemporalProperties properties = context.getBean(TemporalProperties.class);
			assertThat(properties.endpoint()).isEqualTo("localhost:7233");
			assertThat(properties.namespace()).isEqualTo("default");
			assertThat(properties.taskQueue()).isEqualTo("pi-flow");
		});
	}

	@Test
	void bindsOverriddenValues() {
		runner.withPropertyValues(
				"workflow.temporal.endpoint=temporal:7233",
				"workflow.temporal.namespace=my-ns",
				"workflow.temporal.task-queue=my-queue").run(context -> {
					TemporalProperties properties = context.getBean(TemporalProperties.class);
					assertThat(properties.endpoint()).isEqualTo("temporal:7233");
					assertThat(properties.namespace()).isEqualTo("my-ns");
					assertThat(properties.taskQueue()).isEqualTo("my-queue");
				});
	}

	@TestConfiguration
	@EnableConfigurationProperties(TemporalProperties.class)
	static class BindingConfiguration {
	}
}
