package com.hydroyura.agents.piflow.agent.application;

import com.hydroyura.agents.piflow.agent.domain.AgentDefinition;
import java.util.Optional;

/** Resolves agent names to agent definitions. */
public interface AgentResolver {

    Optional<AgentDefinition> resolve(String name);
}
