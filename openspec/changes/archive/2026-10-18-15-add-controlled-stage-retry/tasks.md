# Tasks

> **1.2 confirmed landed shapes (task 4 depends on these):**
> - `StageOutcome` is a record with fields `stage`, `success`, `output`, `verdict`, `findings`, `verificationStatus`, `changeId`, `attemptKind`; factories `success`/`failure`/`reviewed`/`verified`/`changeProposed` default `attemptKind` to `INITIAL`, plus new `technicalRetry`/`contentCorrection` factories and a `withAttemptKind(kind)` copier.
> - `VerificationResult(status, report, truncated)` carries `VerificationStatus` (`PASSED`/`FAILED`/`NOT_CONFIGURED`/`INFRASTRUCTURE_ERROR`). The distinguishing signal for task 4 is the status itself: `FAILED` means the command started and completed within its timeout (nonzero exit → content correction); `INFRASTRUCTURE_ERROR` means the process did not start or timed out (→ technical failure). No extra report flag is required.

## 1. Confirm dependency readiness (strict order, no fallback)

- [x] 1.1 Read `src/main/java/.../workflow/api/StageOutcome.java`, `WorkflowEngineImpl.java`, `Workflow.java`, `WorkflowStatus.java`, and `Verifier.java` and confirm plans 10, 12, 13, and 14 are fully implemented (transition guards and terminal-state checks from 10; `ReviewVerdict`/`AttemptKind`-ready `StageOutcome` shape from 12; `VerificationResult`/`VerificationStatus`/`NOT_CONFIGURED` handling and the deterministic command runner from 13; change-binding from 14). If any of the four is not yet implemented, STOP and implement that dependency first; do not proceed with this change's tasks against an assumed or hedged shape.
- [x] 1.2 Record, as a short note at the top of this file, the confirmed landed shape of `StageOutcome` (its exact fields after plans 12/13) and of `VerificationResult`/`VerificationStatus` (its exact fields, especially whatever signal distinguishes "command did not start/timed out" from "command ran and exited nonzero") that task 4 depends on.

## 2. WorkflowStatus.PAUSED

- [x] 2.1 Add `PAUSED` to `WorkflowStatus`. Grep every exhaustive `switch`/`EnumSet`/equality chain over `WorkflowStatus` in `src/main` and `src/test` and update each to handle `PAUSED` explicitly (not via a `default` that silently misroutes it). Verify by compiling and confirming no switch falls through to an unintended branch for `PAUSED`.
- [x] 2.2 Add `Workflow.markPaused()` setting status to `PAUSED`, following the existing `markAwaitingUser()`/`markFailed()` pattern. Confirm `Workflow.isTerminal()` requires no change (continues to check only `COMPLETED`/`CANCELLED`/`FAILED`) and add a `WorkflowTest` case asserting `isTerminal()` returns `false` for `PAUSED`.
- [x] 2.3 Add a `WorkflowTest`/`WorkflowStatusTest` case asserting `PAUSED` and `AWAITING_USER` are distinct values and that no existing code path sets one when the other is intended (grep for every `markAwaitingUser()`/`markPaused()` call site added by this change's later tasks and confirm each matches the Correction/Approval vs. technical-failure distinction).

## 3. AttemptKind and StageOutcome extension

- [x] 3.1 Add `AttemptKind` enum (`INITIAL`, `TECHNICAL_RETRY`, `CONTENT_CORRECTION`) to `workflow.api`.
- [x] 3.2 Extend `StageOutcome` (per task 1.2's confirmed landed shape) with an `AttemptKind attemptKind` field, defaulting to `INITIAL` in the existing `success(...)`/`failure(...)` factories so no existing call site breaks; grep every `StageOutcome(`/`StageOutcome.success(`/`StageOutcome.failure(` usage in `src/main` and `src/test` and confirm each still compiles.
- [x] 3.3 Add factory methods `StageOutcome.technicalRetry(stage, output)` and `StageOutcome.contentCorrection(stage, output)` (or equivalent, matching the landed shape from task 1.2) producing the corresponding `AttemptKind`. Add a `StageOutcomeTest` asserting the default factories produce `INITIAL` and the new factories produce `TECHNICAL_RETRY`/`CONTENT_CORRECTION` as supplied.

## 4. Verifier failure-cause split (technical vs. content)

- [x] 4.1 Using task 1.2's confirmed `VerificationResult`/`VerificationStatus` shape, implement the split in `WorkflowEngineImpl`'s `VERIFICATION` case: a `FAILED` result where the report indicates the command started and completed within its timeout (nonzero exit) SHALL be classified `CONTENT_CORRECTION` and loop to `IMPLEMENTATION`; a `FAILED` (or equivalent) result where the report indicates the process did not start, or did not complete within its timeout, SHALL be classified as a technical failure and routed per task 5's `PAUSED`/budget logic instead of looping. Verify with `WorkflowEngineImplTest` cases `verification_nonzeroExitWithinTimeout_isContentCorrection` and `verification_processStartOrTimeoutFailure_isTechnicalFailure`, each using a `Verifier` stub constructing the two report shapes distinctly.
- [x] 4.2 Confirm `NOT_CONFIGURED` routing (from plan 13) is unchanged by this split: add a `WorkflowEngineImplTest` case asserting a `NOT_CONFIGURED` result still pauses at `AWAITING_USER` (not `PAUSED`, not a content correction) exactly as plan 13 specifies.
- [x] 4.3 Reclassify `INFRASTRUCTURE_ERROR` routing: unlike plan 13's original behavior (which paused `INFRASTRUCTURE_ERROR` at `AWAITING_USER` alongside `NOT_CONFIGURED`), route an `INFRASTRUCTURE_ERROR` verification result through the same technical-failure/`PAUSED`/budget path as any other technical failure (task 6), per the `workflow-orchestration` spec delta's MODIFIED "Unconfigured or broken verification configuration..." requirement. Verify with `WorkflowEngineImplTest` cases `verification_infrastructureError_withBudgetRemaining_pausesAtPausedNotAwaitingUser` and `verification_infrastructureError_exhaustingBudget_marksFailed`, each using a `Verifier` stub returning `INFRASTRUCTURE_ERROR`.

## 5. Budget configuration and per-workflow counters

- [x] 5.1 Add `RetryProperties` (`@ConfigurationProperties(prefix = "pi-flow.retry")`) with `int maxTechnicalRetries` (default 3) and `int maxContentCorrections` (default 5). Verify with a `RetryPropertiesTest` asserting defaults apply when unbound and configured values are read correctly from a test `application.yaml` fragment.
- [x] 5.2 Add a documented example of `pi-flow.retry.*` configuration (commented out, using the documented defaults) to `src/main/resources/application.yaml`. Verify the application still starts with no explicit retry configuration.
- [x] 5.3 Add a per-stage technical-retry counter and a per-workflow content-correction counter to `Workflow`, with methods to read/increment each and to reset the technical-retry counter when the workflow moves to a new stage via a successful outcome. Verify with `WorkflowTest` cases: incrementing and reading each counter, and the technical-retry counter resetting to zero after `moveTo(...)` following a success.
- [x] 5.4 Remove `WorkflowEngineImpl.MAX_ADVANCE_STEPS`'s role as the product-facing budget. Either remove it entirely in favor of the two new counters, or retain it strictly as a much-larger, purely defensive infinite-loop safety net not exercised by any of this change's test scenarios. Verify by reading the updated `advance()` and confirming the two new counters, not the old single counter, are what every new test in tasks 6-8 actually exercises.

## 6. Technical-failure routing: PAUSED vs. FAILED at the budget boundary

- [x] 6.1 Update `WorkflowEngineImpl`'s handling of a technical failure for an eligible, non-write stage (`PROJECT_ANALYSIS`, `REVIEW`, `VERIFICATION`'s technical-failure case per task 4): if the stage's technical-retry counter is below `RetryProperties.maxTechnicalRetries`, call `workflow.markPaused()` and record the outcome as `TECHNICAL_RETRY` (or `INITIAL` if this is the stage's first attempt) instead of calling `workflow.markFailed()`. Verify with a `WorkflowEngineImplTest` case `technicalFailure_withBudgetRemaining_pausesInsteadOfFailing`. `CHANGE_PROPOSAL` is a write stage (task 6.3) and is excluded from this non-write path.
- [x] 6.2 Update the same handling so that a technical failure occurring when the counter has already reached `maxTechnicalRetries` calls `workflow.markFailed()` (the pre-existing behavior), not `markPaused()`. Verify with `technicalFailure_exhaustingBudget_marksFailed`, asserting `FAILED` is reached exactly on the attempt that consumes the last budget slot, not earlier and not later.
- [x] 6.3 Confirm `CHANGE_PROPOSAL`'s and `IMPLEMENTATION`'s technical failures are both excluded from the `PAUSED` routing added in 6.1 and instead follow task 7's acknowledgement-gated path. Verify with `WorkflowEngineImplTest` cases asserting a technical `CHANGE_PROPOSAL` failure and a technical `IMPLEMENTATION` failure each do not call `markPaused()` directly from `advance()`'s own dispatch (each is only ever retried via the explicit `retryStage` action in task 7, never auto-paused-and-resumed by `advance()` itself beyond the same budget check).

## 7. General retry action with write-stage acknowledgement

- [x] 7.1 Add `WorkflowView retryStage(WorkflowId id, boolean acknowledgePartialChanges)` to `WorkflowEngine` (naming per design.md D5; `acknowledgePartialChanges` defaults to `false` at every call site except the one explicit CLI path in task 9).
- [x] 7.2 Implement `WorkflowEngineImpl.retryStage(id, acknowledgePartialChanges)`:
  - Reject (throw `IllegalStateException`, no state change, no counter increment) if the workflow is terminal.
  - Reject if the workflow is not currently `PAUSED` at an eligible stage, and not currently `AWAITING_USER` at `VERIFICATION`'s `NOT_CONFIGURED` pause.
  - Reject if the eligible stage is `CHANGE_PROPOSAL` or `IMPLEMENTATION` (either write stage) and `acknowledgePartialChanges` is `false`.
  - Reject if the relevant technical-retry budget is already exhausted.
  - On success: increment the stage's technical-retry counter, record the new attempt with `AttemptKind.TECHNICAL_RETRY`, call `workflow.markInProgress()`, and re-enter `advance(workflow)`.
  - Verify with `WorkflowEngineImplTest` cases: `retryStage_forTechnicallyFailedReview_shouldReRunAndSucceed` (non-write stage, no flag needed), `retryStage_forImplementationWithoutAcknowledgement_shouldBeRejected` (asserts rejection and unchanged counters), `retryStage_forImplementationWithAcknowledgement_shouldReRun` (asserts the retry proceeds), `retryStage_forChangeProposalWithoutAcknowledgement_shouldBeRejected` (asserts rejection and unchanged counters), `retryStage_forChangeProposalWithAcknowledgement_shouldReRun` (asserts the retry proceeds), `retryStage_onTerminalWorkflow_shouldBeRejected` (each of `COMPLETED`/`CANCELLED`/`FAILED`), and `retryStage_onNonPausedNonAwaitingWorkflow_shouldBeRejected`.
- [x] 7.3 Add a `WorkflowEngineImplTest` case `retryStage_rejectedCall_shouldNotConsumeBudget` for each rejection path in 7.2, asserting both the technical-retry counter and the content-correction counter are unchanged after a rejected call.
- [x] 7.4 Add a `WorkflowEngineImplTest` case `retryStage_exhaustingTechnicalBudget_shouldMarkFailed` using a `StageRunner` stub that always fails technically, configured with a small `maxTechnicalRetries`, asserting the workflow reaches `FAILED` after the configured number of attempts and a further `retryStage` call is rejected without changing state further.

## 8. Content-correction loop budget enforcement

- [x] 8.1 Update `WorkflowEngineImpl.advance()`'s `REVIEW`/`VERIFICATION` content-correction routing (per task 4's split) to increment the per-workflow content-correction counter each time it loops back to `IMPLEMENTATION`, record the looping outcome with `AttemptKind.CONTENT_CORRECTION`, and call `workflow.markFailed()` instead of looping once the counter reaches `RetryProperties.maxContentCorrections`. Verify with `WorkflowEngineImplTest` cases `contentCorrectionLoop_withinBudget_shouldContinueLooping` and `contentCorrectionLoop_exhaustingBudget_shouldMarkFailed`.

## 9. Absorb plan 13's retryVerification into retryStage

- [x] 9.1 Remove `retryVerification` (or equivalently named method) from `WorkflowEngine`/`WorkflowEngineImpl` if plan 13 added it; update `FlowCommands` and any test referencing it to call `retryStage` instead. Extend `retryStage`'s eligibility check (task 7.2) to recognize the `VERIFICATION`+`AWAITING_USER`+`NOT_CONFIGURED` pause, internally invoking plan 13's existing configuration-reload-and-recheck logic (reused, not reimplemented) before re-entering `advance(workflow)`. Verify with a `WorkflowEngineImplTest` case `retryStage_afterVerificationNotConfigured_shouldReloadConfigAndReEvaluate`: a `Verifier` stub returning `NOT_CONFIGURED` then `PASSED` after configuration is corrected, asserting the workflow advances to `REVIEW` after `retryStage`.
- [x] 9.2 Add a `WorkflowEngineImplTest`/compilation check confirming no `retryVerification`-shaped method remains on the compiled `WorkflowEngine` interface, and that `retryStage` is the sole retry entry point (read the interface file to confirm).

## 10. FlowCommands wiring

- [x] 10.1 Add a `flow-retry` shell command in `FlowCommands` invoking `engine.retryStage(id, acknowledgePartialChanges)`, exposing an `--acknowledge-partial-changes` flag that maps to the `acknowledgePartialChanges` argument (defaulting to `false` when omitted). Remove any `flow-retry-verification`-equivalent command added by plan 13's implementation, per task 9.1's finding. Verify by compiling the shell command and confirming it is registered per the existing project convention for shell command registration.
- [x] 10.2 Add shell-level tests (or the project's existing convention for testing `FlowCommands`) asserting `flow-retry` without `--acknowledge-partial-changes` rejects a retry against an `IMPLEMENTATION`-paused workflow and separately against a `CHANGE_PROPOSAL`-paused workflow, and that `flow-retry --acknowledge-partial-changes` accepts each.

## 11. Documentation

- [x] 11.1 Update `docs/workflow.md`'s "Stage outcomes"/correction-loop and status sections to describe `PAUSED` (distinct from `AWAITING_USER`), the `flow-retry` command, the `--acknowledge-partial-changes` requirement for both write stages (`CHANGE_PROPOSAL` and `IMPLEMENTATION`), the technical-failure/content-correction distinction (including the verifier exit-code-vs-timeout split), and the two configurable budgets. Verify by reading the file after edit and confirming any stale "Retry / correction limits" open question is resolved rather than left stale.
- [x] 11.2 Document the `pi-flow.retry.*` configuration properties (technical-retry budget, content-correction budget, their defaults) in `docs/system-overview.md` or `docs/workflow.md`, alongside how `pi-flow.agent.*`/`pi-flow.verification.*` are already documented. Verify the new properties follow the same documentation format.
- [x] 11.3 Explicitly document that auto-retry is disabled by default and that this change adds no configuration to enable unattended retry, so operators do not assume one exists.

## 12. Validation

- [x] 12.1 Run `openspec validate 15-add-controlled-stage-retry --strict`, scoped to this change's own directory only, and fix any reported spec formatting issues. Verify the command exits 0.

## 13. Full-history and terminal-immutability verification

- [x] 13.1 Add a `WorkflowEngineImplTest` case `outcomes_afterMultipleRetries_shouldPreserveFullHistoryWithKinds`: drive a workflow through a technical-failure retry (via `retryStage`) followed by a content-correction loop, and assert `view.outcomes()` contains every intermediate attempt (not only the latest), each with the correct `AttemptKind`.
- [x] 13.2 Add a `WorkflowEngineImplTest` case confirming that none of this change's new behavior (budget exhaustion, `PAUSED` transitions, retry rejection) introduces any path that moves a `COMPLETED` or `CANCELLED` workflow to a different status, reusing plan 10's existing terminal-immutability test pattern.
- [x] 13.3 Add a `WorkflowEngineImplTest` case confirming a `FAILED` workflow reached via budget exhaustion rejects every further `retryStage` call and every gate command, consistent with plan 10's terminal-state guarantee.
