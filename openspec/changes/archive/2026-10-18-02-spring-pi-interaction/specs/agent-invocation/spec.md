# Spec Delta

## Purpose

Defines how the pi-flow orchestrator invokes the Pi Coding Agent in JSON mode and captures structured stage outcomes.

## ADDED Requirements

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
