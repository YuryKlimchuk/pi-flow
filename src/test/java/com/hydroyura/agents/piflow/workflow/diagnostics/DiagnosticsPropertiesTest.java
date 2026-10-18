package com.hydroyura.agents.piflow.workflow.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class DiagnosticsPropertiesTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DiagnosticsProperties.class)
    static class TestConfig {
    }

    @Test
    void bind_withDefaults_whenNoProperties() {
        runner.run(context -> {
            DiagnosticsProperties properties = context.getBean(DiagnosticsProperties.class);
            assertThat(properties.maxEntriesPerWorkflow()).isEqualTo(200);
            assertThat(properties.maxReasonLength()).isEqualTo(500);
        });
    }

    @Test
    void bind_withOverrides_shouldApplyConfiguredValues() {
        runner.withPropertyValues(
                        "pi-flow.diagnostics.max-entries-per-workflow=10",
                        "pi-flow.diagnostics.max-reason-length=25")
                .run(context -> {
                    DiagnosticsProperties properties = context.getBean(DiagnosticsProperties.class);
                    assertThat(properties.maxEntriesPerWorkflow()).isEqualTo(10);
                    assertThat(properties.maxReasonLength()).isEqualTo(25);
                });
    }
}
