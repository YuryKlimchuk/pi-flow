package com.hydroyura.agents.piflow.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class RetryPropertiesTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(RetryProperties.class)
    static class TestConfig {
    }

    @Test
    void bind_withDefaults_whenNoProperties() {
        runner.run(context -> {
            RetryProperties properties = context.getBean(RetryProperties.class);
            assertThat(properties.maxTechnicalRetries()).isEqualTo(3);
            assertThat(properties.maxContentCorrections()).isEqualTo(5);
        });
    }

    @Test
    void bind_withOverrides_shouldApplyConfiguredValues() {
        runner.withPropertyValues(
                        "pi-flow.retry.max-technical-retries=2",
                        "pi-flow.retry.max-content-corrections=7")
                .run(context -> {
                    RetryProperties properties = context.getBean(RetryProperties.class);
                    assertThat(properties.maxTechnicalRetries()).isEqualTo(2);
                    assertThat(properties.maxContentCorrections()).isEqualTo(7);
                });
    }
}
