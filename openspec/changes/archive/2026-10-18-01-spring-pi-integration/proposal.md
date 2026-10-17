# Proposal

## Why

The `00-base-architecture` change deferred how the pi-flow orchestrator (Spring Boot) communicates with the Pi Coding Agent, recording it as an open question. That decision now blocks any implementation of the workflow engine, because the agent-integration boundary is the first integration point. This change evaluates every available Pi integration mode, lays out pros and cons, and prepares a recommendation so the choice can be made and recorded.

## What Changes

- Add a comparison of all Spring ↔ Pi interaction variants, with pros/cons and a recommendation, in this change's `design.md`.
- Record the selected variant (and its rationale) in the architecture docs (`docs/system-overview.md`, `docs/workflow.md`) once the choice is made.
- Resolve the "Spring ↔ agent communication" open question left by `00-base-architecture`.

## Capabilities

### New Capabilities

None — this change is analysis/decision plus documentation updates. No spec-level behavior is introduced yet (implementation is deferred).

### Modified Capabilities

None.

## Impact

- No code, API, or dependency changes in this change.
- Updates `docs/system-overview.md` and `docs/workflow.md` to record the chosen integration mechanism.
- Establishes the integration contract that the future agent-integration port must follow.
