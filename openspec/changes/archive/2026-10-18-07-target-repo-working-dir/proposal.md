# Proposal

## Why

The orchestrator records a target repository for each workflow, but the Pi runner has no way to launch the agent inside it. Pi currently runs in the process's own working directory (or a single global default), so agent stages do not actually operate on the target project. The target repository path is only embedded in the prompt text.

## What Changes

- Extend the `PiRunner` port with an overload that accepts a working directory per run.
- Make `AgentStageRunner` pass `workflow.getTargetRepo()` as the working directory for agent stages.
- Make `JsonModePiRunner` launch the Pi process in the provided directory, falling back to the configured global default when none is given.
- Keep the existing global `pi-flow.agent.working-directory` property as the fallback default.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `agent-invocation`: the runner SHALL accept an optional working directory per run and launch the Pi process in it, so agent stages execute inside the target repository.

## Impact

- `agent.api.PiRunner` gains an overload accepting a working directory.
- `agent.infrastructure.JsonModePiRunner` resolves the process directory per run (explicit value over global config).
- `workflow.infrastructure.AgentStageRunner` passes the workflow's target repository as the working directory.
- Existing agent runner tests updated; new tests cover the per-run directory precedence.
