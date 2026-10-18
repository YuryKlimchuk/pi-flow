package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class VerificationPropertiesTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(VerificationProperties.class)
    static class TestConfig {
    }

    @Test
    void bind_withDefaults_whenNoProperties() {
        runner.run(context -> {
            VerificationProperties properties = context.getBean(VerificationProperties.class);
            assertThat(properties.configPath()).isEqualTo(".pi-flow/verify.json");
        });
    }

    @Test
    void bind_withOverride_shouldApplyConfiguredPath() {
        runner.withPropertyValues("pi-flow.verification.config-path=.repo/verify.json")
                .run(context -> {
                    VerificationProperties properties = context.getBean(VerificationProperties.class);
                    assertThat(properties.configPath()).isEqualTo(".repo/verify.json");
                });
    }
}
