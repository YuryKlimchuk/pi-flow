# Proposal

## Why

pi-flow is a greenfield local workflow orchestrator that coordinates Pi Coding Agent sessions through an explicit, human-gated process. Before any implementation, the project needs an agreed base architecture: the system participants, their responsibilities, and the end-to-end workflow.

## What Changes

- Add `docs/system-overview.md` — the system participants (user, pi-flow orchestrator, Pi Coding Agent, database, target repository), their responsibilities, and how they relate (Mermaid component diagram).
- Add `docs/workflow.md` — the step-by-step workflow with the actions of each participant (Mermaid sequence diagram).
- Introduce the `00-base-architecture` change that owns these documents.

## Capabilities

### New Capabilities

None — this change establishes architecture documentation only. Formal requirements (specs) are deferred to follow-up changes once the architecture is agreed.

### Modified Capabilities

None.

## Impact

- No code, API, or dependency changes.
- Introduces the `docs/` directory.
- Establishes the reference architecture that subsequent changes must follow.
