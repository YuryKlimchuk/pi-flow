package com.hydroyura.agents.piflow.agent.infrastructure;

import com.hydroyura.agents.piflow.agent.application.AgentResolver;
import java.nio.file.Path;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the agent resolver with the standard Pi agent directories. */
@Configuration
public class AgentIntegrationConfig {

    @Bean
    AgentResolver agentResolver() {
        Path projectAgents = Path.of(".pi", "agents");
        Path userAgents = Path.of(System.getProperty("user.home"), ".pi", "agent", "agents");
        return new FileAgentResolver(List.of(projectAgents, userAgents));
    }
}
