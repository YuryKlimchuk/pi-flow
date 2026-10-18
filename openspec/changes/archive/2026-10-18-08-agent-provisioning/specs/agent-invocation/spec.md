# Spec Delta

## MODIFIED Requirements

### Requirement: Resolve agent definition
Given an agent name, the runner SHALL load that agent's definition from the orchestrator's per-agent resources (system prompt and tools from `system.md`, skills from the `skills/` subfolder) and resolve its model through the configured tier mapping.

#### Scenario: agent found
- **WHEN** an agent with the given name exists in the orchestrator resources
- **THEN** the runner loads its system prompt, tools, and skills, and resolves its model from the configured tier

#### Scenario: agent not found
- **WHEN** no agent with the given name exists in the orchestrator resources
- **THEN** the runner returns a FAILURE outcome with a message naming the missing agent

## ADDED Requirements

### Requirement: Resolve model by tier
The runner SHALL resolve an agent's model id through the configured tier mapping.

#### Scenario: tier mapped
- **WHEN** the agent's tier is present in the configured tier mapping
- **THEN** the runner uses the mapped model id when invoking Pi

#### Scenario: tier not mapped
- **WHEN** the agent's tier is missing or has no configured model id
- **THEN** the runner returns a FAILURE outcome with a message naming the unresolved tier

### Requirement: Pass skills to agent
When an agent defines skills, the runner SHALL pass them to Pi.

#### Scenario: skills present
- **WHEN** the agent's resource folder contains skills
- **THEN** the runner invokes Pi with `--skill` for each discovered skill

#### Scenario: no skills
- **WHEN** the agent has no skills
- **THEN** the runner invokes Pi without `--skill`
