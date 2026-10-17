# Proposal

## Why

The workflow in `docs/workflow.md` assumes per-stage agent roles but leaves their concrete set undefined. Each stage needs a defined role (responsibility, tooling, and a model-cost tier) so the orchestrator can configure Pi sessions consistently. This change analyzes the workflow, fixes the role set, and records it in a machine-readable file.

## What Changes

- Analyze the workflow and define the set of agent roles it requires.
- Map each role to a model-cost tier (expensive / medium / cheap) with rationale.
- Add `docs/agent-roles.md` — a reference of roles, stages, and model tiers. Agent descriptions live in Pi (`~/.pi/agent/agents/*.md`); pi-flow selects an agent by name per stage.
- Resolve the "Agent roles" open question left by `00-base-architecture`.

## Capabilities

### New Capabilities

None — this change introduces configuration data only (no spec-level behavior yet).

### Modified Capabilities

None.

## Impact

- New reference doc `docs/agent-roles.md`.
- No code or application-resource changes.
- `docs/workflow.md` stage tables updated to use the defined role names.
