package com.hydroyura.agents.piflow.agent.application;

import com.hydroyura.agents.piflow.agent.domain.AgentDefinition;
import java.util.Objects;

/**
 * Result of resolving an agent by name: either a definition or an error message.
 */
public record AgentResolution(AgentDefinition definition, String error) {

    public static AgentResolution ok(AgentDefinition definition) {
        return new AgentResolution(Objects.requireNonNull(definition, "definition must not be null"), null);
    }

    public static AgentResolution error(String message) {
        return new AgentResolution(null, Objects.requireNonNull(message, "message must not be null"));
    }

    public boolean isResolved() {
        return definition != null;
    }
}
