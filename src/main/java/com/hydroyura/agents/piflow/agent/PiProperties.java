package com.hydroyura.agents.piflow.agent;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for the agent integration, bound from {@code pi-flow.agent.*}. */
@ConfigurationProperties(prefix = "pi-flow.agent")
public record PiProperties(String command, Duration timeout, String workingDirectory) {

    public PiProperties {
        if (command == null || command.isBlank()) {
            command = "pi";
        }
        if (timeout == null) {
            timeout = Duration.ofMinutes(5);
        }
    }
}
