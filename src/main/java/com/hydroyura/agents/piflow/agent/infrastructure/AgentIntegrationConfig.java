package com.hydroyura.agents.piflow.agent.infrastructure;

import com.hydroyura.agents.piflow.agent.application.AgentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** Wires the resource-based agent resolver. */
@Configuration
public class AgentIntegrationConfig {

    @Bean
    AgentResolver agentResolver(AgentProperties properties) {
        return new ResourceAgentResolver(properties, new PathMatchingResourcePatternResolver());
    }
}
