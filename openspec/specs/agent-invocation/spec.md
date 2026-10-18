# agent-invocation Specification

## Purpose
Defines how the pi-flow orchestrator invokes the Pi Coding Agent in JSON mode and captures structured stage outcomes.

## Requirements

### Requirement: Invoke agent in JSON mode
The orchestrator SHALL launch the Pi CLI as a child process in JSON mode with a prompt and capture its JSONL output.

#### Scenario: successful run
- **WHEN** Pi completes the run successfully
- **THEN** the runner returns the final assistant text and a SUCCESS outcome

#### Scenario: failed run
- **WHEN** Pi exits nonzero or reports a failed or aborted response
- **THEN** the runner returns a FAILURE outcome distinguishable from success

### Requirement: Assemble structured result from events
The runner SHALL assemble the JSONL event stream into a structured result.

#### Scenario: delta assembly
- **WHEN** `message_update` delta events arrive
- **THEN** the runner accumulates them and the final text reflects the authoritative `message_end` message

#### Scenario: completion signal
- **WHEN** the `agent_settled` event arrives
- **THEN** the run is considered complete

### Requirement: Configurable command
The Pi executable path SHALL be configurable so tests and environments can substitute it.

#### Scenario: custom command
- **WHEN** a custom Pi command is configured
- **THEN** the runner invokes that command instead of the default

### Requirement: Run timeout
The runner SHALL enforce a timeout so a hung agent does not hang the orchestrator.

#### Scenario: timeout
- **WHEN** the Pi process does not finish within the configured timeout
- **THEN** the runner terminates it and returns a TIMEOUT outcome

### Requirement: Resolve agent definition
Given an agent name, the runner SHALL load that agent's definition from the orchestrator's per-agent resources (system prompt and tools from `system.md`, skills from the `skills/` subfolder) and resolve its model through the configured tier mapping.

#### Scenario: agent found
- **WHEN** an agent with the given name exists in the orchestrator resources
- **THEN** the runner loads its system prompt, tools, and skills, and resolves its model from the configured tier

#### Scenario: agent not found
- **WHEN** no agent with the given name exists in the orchestrator resources
- **THEN** the runner returns a FAILURE outcome with a message naming the missing agent

### Requirement: Invoke with agent configuration
When an agent is selected, the runner SHALL pass the agent's model, tools, and system prompt to Pi.

#### Scenario: model and tools applied
- **WHEN** the agent defines a model and tools
- **THEN** the runner invokes Pi with `--model` and `--tools` set from the agent

#### Scenario: system prompt appended
- **WHEN** the agent has a non-empty system prompt
- **THEN** the runner appends it to Pi's system prompt

### Requirement: Run in a specified working directory
The runner SHALL accept an optional working directory per run and launch the Pi process in that directory when one is provided.

#### Scenario: working directory provided
- **WHEN** the runner is invoked with a working directory
- **THEN** the Pi process is launched with that directory as its working directory

#### Scenario: no working directory provided
- **WHEN** the runner is invoked without a working directory
- **THEN** the Pi process is launched with the configured default working directory, or the process's own directory if none is configured

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
