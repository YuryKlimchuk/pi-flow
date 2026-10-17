# Proposal

## Why

The architecture, roles, and agent invocation are ready, but nothing actually runs the workflow from `docs/workflow.md`. pi-flow needs the orchestration engine — a synchronous state machine over the workflow stages — to drive stages, enforce approval gates, handle correction loops, and persist state.

## What Changes

- Add a `workflow` application module (Modulith).
- Add a hand-rolled enum state machine: `WorkflowStage`, `WorkflowStatus`, and a `Workflow` aggregate.
- Add a `WorkflowRepository` port with a default in-memory Map implementation.
- Add a synchronous `WorkflowEngine` that advances stages, stops at gates, and loops on corrections.
- Add a `StageRunner` port and an agent-backed implementation mapping stages to roles.
- Add `flow` shell commands to drive the workflow.

## Capabilities

### New Capabilities

- `workflow-orchestration`: running the pi-flow workflow as a synchronous, persisted state machine.

### Modified Capabilities

None.

## Impact

- New `workflow` module (api/application/domain/infrastructure).
- `shell` module gains `flow` commands and depends on `workflow.api`.
- No new external dependencies (repository is in-memory).
