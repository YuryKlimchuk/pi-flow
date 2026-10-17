# Spec Delta

## ADDED Requirements

### Requirement: Resolve agent definition
Given an agent name, the runner SHALL load that agent's definition (model, tools, and system prompt) from the configured agent directories.

#### Scenario: agent found
- **WHEN** an agent with the given name exists
- **THEN** the runner loads its model, tools, and system prompt

#### Scenario: agent not found
- **WHEN** no agent with the given name exists
- **THEN** the runner returns a FAILURE outcome with a message naming the missing agent

### Requirement: Invoke with agent configuration
When an agent is selected, the runner SHALL pass the agent's model, tools, and system prompt to Pi.

#### Scenario: model and tools applied
- **WHEN** the agent defines a model and tools
- **THEN** the runner invokes Pi with `--model` and `--tools` set from the agent

#### Scenario: system prompt appended
- **WHEN** the agent has a non-empty system prompt
- **THEN** the runner appends it to Pi's system prompt
