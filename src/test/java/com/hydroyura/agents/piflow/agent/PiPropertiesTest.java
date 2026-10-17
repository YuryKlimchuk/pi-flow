package com.hydroyura.agents.piflow.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class PiPropertiesTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PiProperties.class)
    static class TestConfig {
    }

    @Test
    void bind_withDefaults_whenNoProperties() {
        runner.run(context -> {
            PiProperties properties = context.getBean(PiProperties.class);
            assertThat(properties.command()).isEqualTo("pi");
            assertThat(properties.timeout()).isEqualTo(Duration.ofMinutes(5));
            assertThat(properties.workingDirectory()).isNull();
        });
    }

    @Test
    void bind_withOverrides_shouldApplyValues() {
        runner.withPropertyValues(
                        "pi-flow.agent.command=/opt/fake-pi",
                        "pi-flow.agent.timeout=10s",
                        "pi-flow.agent.working-directory=/tmp/repo")
                .run(context -> {
                    PiProperties properties = context.getBean(PiProperties.class);
                    assertThat(properties.command()).isEqualTo("/opt/fake-pi");
                    assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(properties.workingDirectory()).isEqualTo("/tmp/repo");
                });
    }
}
