# Proposal

## Why

`WorkflowEngineImpl` gates (`approve`, `reject`, `requestRevisions`, `requestFurtherWork`) only check `currentStage`, never `status`. Because `reject()` marks the workflow `CANCELLED` without moving it off `VALIDATION_APPROVAL`, a later `approve()` call on the same workflow id passes the stage check (still `VALIDATION_APPROVAL`), silently resurrecting a cancelled workflow and driving it into `IMPLEMENTATION`. The same stage-only check lets any gate command fire while the workflow is `IN_PROGRESS` (mid-`advance()`) instead of `AWAITING_USER`, and lets commands fire against workflows already `COMPLETED` or `FAILED`. Separately, `advance()` has no exception handling around `stageRunner.run(...)` or `verifier.verify(...)`: any runtime exception from those ports propagates uncaught instead of being recorded as a failed stage outcome, leaving the persisted workflow in an inconsistent state.

## What Changes

- **BREAKING**: Gate commands (`approve`, `reject`, `requestRevisions`, `requestFurtherWork`) SHALL require both the expected `WorkflowStage` AND `WorkflowStatus.AWAITING_USER` before acting; today they check stage only.
- Reject SHALL be rejected (no-op, exception) when the workflow is not at the `VALIDATION_APPROVAL` gate while `AWAITING_USER`, closing the resurrection path where a cancelled-but-not-moved workflow re-enters `approve()`.
- Terminal statuses (`COMPLETED`, `CANCELLED`, `FAILED`) SHALL be immutable to every gate command and to `advance()` for this change. No automatic or manual transition out of a terminal status is introduced here; an explicit, user-approved retry/resume capability is deferred to a future change (see Non-goals in design.md).
- `advance()` SHALL catch exceptions thrown by `StageRunner.run(...)` and `Verifier.verify(...)` and record a failed `StageOutcome` (and transition to `FAILED`) instead of letting the exception propagate out of the engine. `Error` subtypes representing fatal JVM conditions (e.g. `OutOfMemoryError`, `StackOverflowError`) SHALL NOT be caught by this handling and continue to propagate.
- Add negative/guard tests covering: reject-then-approve resurrection, gate calls while `IN_PROGRESS`, gate calls on terminal workflows, and stage-runner/verifier exceptions surfacing as failed outcomes.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `workflow-orchestration`: approval/correction/acceptance gate requirements gain an explicit status precondition (`AWAITING_USER`), terminal-state immutability, and exception-safe stage advancement that records failure instead of propagating.

## Impact

- `src/main/java/.../workflow/application/WorkflowEngineImpl.java`: gate methods gain status checks; `advance()` gains try/catch around stage execution and verification.
- `src/main/java/.../workflow/domain/Workflow.java`: no field changes expected; may gain a guard helper (e.g. `requireAwaitingUser`) used by the engine - decided in design.md.
- Tests: `WorkflowEngineImplTest`, `WorkflowTest` gain negative cases; no change to `WorkflowRepository`, `StageRunner`, or `Verifier` port signatures.
- No changes to `agent-invocation` capability, shell commands' external signatures, or persisted schema.
