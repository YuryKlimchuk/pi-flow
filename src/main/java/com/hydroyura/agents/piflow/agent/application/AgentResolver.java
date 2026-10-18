package com.hydroyura.agents.piflow.agent.application;

/** Resolves agent names to agent definitions. */
public interface AgentResolver {

    AgentResolution resolve(String name);
}
