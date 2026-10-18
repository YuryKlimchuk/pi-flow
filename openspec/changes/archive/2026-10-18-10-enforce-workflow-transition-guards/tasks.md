# Tasks

## 1. Domain guard helper

- [x] 1.1 Add a guard method to `Workflow` (e.g. `requireAwaitingUserAt(WorkflowStage expectedStage)`) that throws `IllegalStateException` unless `currentStage == expectedStage && status == WorkflowStatus.AWAITING_USER`; verify with a new unit test in `WorkflowTest` asserting both a pass case (`VALIDATION_APPROVAL` + `AWAITING_USER`) and a fail case (correct stage, `IN_PROGRESS` status) throw/pass as expected.
- [x] 1.2 Add a `WorkflowTest` case asserting the guard rejects a correct-stage-but-terminal-status workflow (e.g. `VALIDATION_APPROVAL` + `CANCELLED`), confirming terminal immutability at the domain level.

## 2. Gate commands use the guard

- [x] 2.1 Update `WorkflowEngineImpl.approve()` to call the new guard for both the `VALIDATION_APPROVAL` and `ACCEPTANCE` branches before mutating state; verify `WorkflowEngineImplTest.approveThenAccept_shouldComplete` still passes unchanged.
- [x] 2.2 Update `WorkflowEngineImpl.reject()` to call the guard for `VALIDATION_APPROVAL`; verify `WorkflowEngineImplTest.reject_shouldCancel` still passes unchanged.
- [x] 2.3 Update `WorkflowEngineImpl.requestRevisions()` to call the guard for `VALIDATION_APPROVAL`; verify `WorkflowEngineImplTest.requestRevisions_shouldReturnToProposalAndGate` still passes unchanged.
- [x] 2.4 Update `WorkflowEngineImpl.requestFurtherWork()` to call the guard for `ACCEPTANCE`; verify existing review/verification-loop tests in `WorkflowEngineImplTest` still pass unchanged.
- [x] 2.5 Add a negative test `reject_thenApprove_shouldNotResurrectCancelledWorkflow`: start a workflow, reject it, then call `approve()` again on the same id and assert it throws `IllegalStateException` and the workflow remains `CANCELLED` at `VALIDATION_APPROVAL`.
- [x] 2.6 Add a negative test `approve_whileInProgress_shouldBeRejected`: construct a workflow at `VALIDATION_APPROVAL` with status forced to `IN_PROGRESS` (via a `StageRunner` that never reaches the gate, or by directly exercising `Workflow` state before calling the engine) and assert `approve()` throws and state is unchanged.
- [x] 2.7 Add negative tests for each gate command against each terminal status (`COMPLETED`, `CANCELLED`, `FAILED`): `approve_onCompletedWorkflow_shouldBeRejected`, `reject_onCancelledWorkflow_shouldBeRejected`, `requestFurtherWork_onFailedWorkflow_shouldBeRejected`; assert `IllegalStateException` and unchanged state in each.

## 3. Exception-safe stage advancement

- [x] 3.1 Wrap the `stageRunner.run(stage, workflow)` call inside `advance()`'s agent-stage branch (`PROJECT_ANALYSIS, CHANGE_PROPOSAL, IMPLEMENTATION, REVIEW`) in a `try { ... } catch (RuntimeException e) { ... }` that records `StageOutcome.failure(stage, <message naming the exception type and getMessage()>)` and calls `workflow.markFailed()`; verify with a new `WorkflowEngineImplTest` case using a `StageRunner` stub that throws a `RuntimeException` for `IMPLEMENTATION`, asserting the resulting outcome is recorded as failed and `view.status()` is `FAILED`.
- [x] 3.2 Wrap the `verifier.verify(workflow)` call inside `advance()`'s `VERIFICATION` branch the same way; verify with a new `WorkflowEngineImplTest` case using a `Verifier` stub that throws, asserting a failed `StageOutcome` is recorded and `view.status()` is `FAILED`.
- [x] 3.3 Confirm (by code review and a focused test) that the catch clauses in 3.1/3.2 use `RuntimeException`, not `Exception` or `Throwable`, so an `Error` subtype is never caught; add a test that a `StageRunner` throwing `AssertionError` (a convenient `Error` subtype for testing without crashing the JVM) propagates out of `advance()`/`start()` uncaught, confirmed by asserting the test method itself expects that error via `assertThatThrownBy(...).isInstanceOf(AssertionError.class)` or equivalent, run in isolation so a genuinely fatal error is never deliberately thrown in the suite.

## 4. Documentation and spec alignment

- [x] 4.1 Update `docs/workflow.md` if it documents gate command preconditions, so the stage-only description there is corrected to state both the stage AND `AWAITING_USER` status are required; if `docs/workflow.md` does not currently describe gate preconditions at this level of detail, add a short note instead of leaving stale wording. Verify by reading the file after edit and confirming no remaining text implies a stage-only check.
- [x] 4.2 Run `openspec validate 10-enforce-workflow-transition-guards --strict` and fix any reported spec formatting issues; verify the command exits 0.
