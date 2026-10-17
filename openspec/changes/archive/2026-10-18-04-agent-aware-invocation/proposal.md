# Proposal

## Why

Change `02-spring-pi-interaction` proved that pi-flow can invoke Pi in JSON mode, but it can only pass a prompt — it cannot run Pi under a specific agent role. The roles are now defined in `docs/agent-roles.md`, and the corresponding agents are configured in Pi (`~/.pi/agent/agents/*.md`). pi-flow needs to resolve an agent by name and apply its model, tools, and system prompt when launching a stage.

## What Changes

- Add agent resolution: read an agent's markdown definition (YAML frontmatter + system prompt) by name.
- Extend the agent integration to pass the agent's `--model`, `--tools`, and `--append-system-prompt` to Pi.
- Extend `PiRunner` with an agent-aware run method, keeping the existing prompt-only method working.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `agent-invocation`: add requirements for resolving an agent definition and invoking Pi with that agent's configuration.

## Impact

- New main-source classes for the agent definition and resolver, plus frontmatter parsing.
- `PiRunner` and `JsonModePiRunner` extended.
- No new external dependencies (frontmatter parsed with SnakeYAML, already on the classpath).
