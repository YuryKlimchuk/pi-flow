package com.hydroyura.agents.piflow.agent.infrastructure;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Agent provisioning configuration, bound from {@code pi-flow.agents.*}.
 *
 * @param tiers tier name -> model id
 * @param roles role name -> tier name
 */
@ConfigurationProperties(prefix = "pi-flow.agents")
public record AgentProperties(Map<String, String> tiers, Map<String, String> roles) {

    public AgentProperties {
        tiers = tiers != null ? Map.copyOf(tiers) : Map.of();
        roles = roles != null ? Map.copyOf(roles) : Map.of();
    }

    public String modelFor(String tier) {
        return tiers.get(tier);
    }

    public String tierFor(String role) {
        return roles.get(role);
    }
}
