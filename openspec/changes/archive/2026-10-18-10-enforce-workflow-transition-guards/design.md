# Design

## Context

See proposal.md - Why for the resurrection bug and uncaught-exception gap. Current state, read from `WorkflowEngineImpl`, `Workflow`, and `WorkflowStatus`:

- `approve()`, `reject()`, `requestRevisions()`, `requestFurtherWork()` each switch/branch only on `workflow.getCurrentStage()`. None checks `workflow.getStatus()`.
- `reject()` calls `workflow.markCancelled()` but never calls `workflow.moveTo(...)`, so `currentStage` stays `VALIDATION_APPROVAL` after cancellation.
- `advance()` loops while `status == IN_PROGRESS`, calling `stageRunner.run(...)` for agent stages and `verifier.verify(...)` for `VERIFICATION`, with no try/catch around either call.
- `WorkflowStatus` has five values: `IN_PROGRESS`, `AWAITING_USER`, `COMPLETED`, `CANCELLED`, `FAILED`. `Workflow.isTerminal()` already treats `COMPLETED`, `CANCELLED`, `FAILED` as terminal.
- Gate methods currently throw `IllegalStateException` for a wrong-stage call; tests assert on this behavior for the happy paths (`WorkflowEngineImplTest`).

## Goals / Non-Goals

**Goals:**
- Close the reject-then-approve resurrection path by making every gate check both stage and status.
- Make terminal statuses immutable to all gate commands and to `advance()`.
- Make `advance()` exception-safe for the two pluggable ports (`StageRunner`, `Verifier`) without catching fatal JVM errors.
- Keep the fix local to `workflow.application` and `workflow.domain`; no port signature changes.

**Non-Goals:**
- Designing the future retry/resume capability referenced in proposal.md (tracked as plan 15). This change only guarantees terminal states are immutable; it does not add any transition out of them.
- Changing `WorkflowStatus` enum values or `WorkflowStage` enum values.
- Changing persistence (`WorkflowRepository`) semantics or the `StageRunner`/`Verifier` port interfaces.
- Changing shell command (`FlowCommands`) argument/return shapes; only the exception a command surfaces when a guard rejects it may change in message text.

## Decisions

**D1: Guard check shape - explicit `requireStageAndStatus` helper on `Workflow`, not duplicated conditionals in the engine.**
Add a package-visible or public helper on `Workflow`, e.g. `requireAwaitingUserAt(WorkflowStage expectedStage)`, that throws `IllegalStateException` when `currentStage != expectedStage || status != WorkflowStatus.AWAITING_USER`. Each gate method in `WorkflowEngineImpl` calls this once instead of a bespoke `if`. Alternative considered: inline the stage+status check in each of the four gate methods in `WorkflowEngineImpl`. Rejected because it duplicates the same two-part condition four times and is easy to miss updating consistently; a single domain-level guard keeps the invariant in one place and makes `Workflow` the source of truth for its own legal transitions, consistent with the existing `markX()`/`moveTo()` methods already living there.

**D2: Reject fixes the resurrection bug by moving stage, not only by checking status elsewhere.**
`reject()` SHALL call `workflow.moveTo(WorkflowStage.COMPLETED)` is rejected as a choice (COMPLETED implies success); instead `reject()` keeps `currentStage` as `VALIDATION_APPROVAL` (no stage representing "cancelled" exists, and adding one is out of scope) and relies purely on the status guard: once `markCancelled()` sets status to `CANCELLED`, every future gate call - including a second `approve()` - fails the `status == AWAITING_USER` check regardless of what `currentStage` reads. This is why D1's guard must check status, not stage alone: it is the status check, not a stage change, that actually closes the resurrection path. Confirmed by proposal.md and the new scenario "reject cannot resurrect a cancelled workflow".

**D3: Terminal immutability enforced by the same guard, plus a defensive check in `advance()`.**
Because `requireAwaitingUserAt` already rejects any call when status is not `AWAITING_USER`, and no terminal status is ever `AWAITING_USER`, D1's guard alone makes all four gate methods reject calls against `COMPLETED`/`CANCELLED`/`FAILED` workflows. `advance()` is additionally guarded by its existing `while (status == IN_PROGRESS)` loop condition, which already prevents advancing a terminal workflow; no change needed there beyond the exception handling in D4. Alternative considered: a separate `isTerminal()` check duplicated in each gate method. Rejected as redundant with D1's guard - `AWAITING_USER` and "terminal" are mutually exclusive by construction, so one guard covers both.

**D4: Exception handling wraps only the two pluggable, stage-level calls, not every statement in `advance()`.**
Wrap `stageRunner.run(stage, workflow)` (for `PROJECT_ANALYSIS`, `CHANGE_PROPOSAL`, `IMPLEMENTATION`, `REVIEW`) and `verifier.verify(workflow)` (for `VERIFICATION`) each in their own try/catch inside the existing `switch` cases. On catching `RuntimeException` (and checked exceptions if a port ever declares one - today neither does), record `StageOutcome.failure(stage, <message naming the exception>)` and call `workflow.markFailed()`, mirroring the existing non-exceptional failure path for non-REVIEW stages. Catch `RuntimeException`, not `Throwable` or `Exception`, so `Error` subtypes (`OutOfMemoryError`, `StackOverflowError`, etc.) are never caught here and continue to propagate out of `advance()`, satisfying the "no fatal VM error catching" requirement. Alternative considered: a single try/catch around the whole `switch` statement. Rejected because it would also swallow exceptions from the deterministic branches (`TASK_SETUP`, `COMPLETED`, the gate-pausing branches), which do not call untrusted ports and should keep failing loudly if they ever throw (that would indicate an engine bug, not a stage/verifier bug).

**D5: Error message content for exception-triggered failures.**
The recorded `StageOutcome.failure(stage, message)` message SHALL include the exception's class simple name and `getMessage()` (when present) so the failure is diagnosable from `WorkflowView.outcomes()` alone, without needing to correlate with logs. Exact string format is an implementation detail left to tasks.md; the spec only requires that a failed outcome is recorded, not its literal text.

## Risks / Trade-offs

- [Risk] Making gate checks stricter (stage AND status) could break any caller that previously relied on calling a gate command while `IN_PROGRESS` and having it queue/no-op silently via the stage check alone → Mitigation: current behavior already throws `IllegalStateException` for a stage mismatch, so callers already handle an exception from these methods; the new status check reuses the same exception type and call sites, verified by inspecting `FlowCommands` which simply propagates the exception.
- [Risk] Catching `RuntimeException` broadly around `stageRunner.run`/`verifier.verify` could mask a programming error in the port implementation that would otherwise be a loud test failure → Mitigation: the failed `StageOutcome` records the exception type and message, and the workflow transitions to `FAILED` (a terminal, inspectable state) rather than silently continuing; tests assert the message is present so regressions are caught by assertions, not swallowed logs.
- [Risk] Introducing a shared guard helper on `Workflow` changes the shape of `IllegalStateException` messages used in today's tests (`WorkflowEngineImplTest` doesn't currently assert on message text, confirmed by reading the test file) → Mitigation: no behavioral risk since no existing test asserts exact exception message text; new negative tests only assert the exception type and resulting state.
