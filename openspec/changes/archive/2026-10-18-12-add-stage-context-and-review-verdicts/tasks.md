# Tasks

## 0. Preconditions

- [x] 0.1 Confirm plan 10 (`10-enforce-workflow-transition-guards`) and plan 11 (`11-harden-pi-result-contract`) are both fully implemented in `src/main/java` (guard helper present on `Workflow`; `PiJsonlParser`/`JsonModePiRunner` hardened completion contract present) before starting any other task in this change; if either is not yet implemented, stop and implement it first rather than substituting an inline equivalent here.

## 1. StageContext and feedback storage

- [x] 1.1 Add `StageContext` record to `workflow.application` (task, targetRepo, `Optional<String> analysisFindings`, `Optional<String> proposalOutput`, `Optional<String> pendingFeedback`) per design.md D1; verify with a new unit test constructing it with and without optional fields present.
- [x] 1.2 Add `pendingFeedback` field, `recordPendingFeedback(String feedback)`, and `consumePendingFeedback()` (returns current value, resets to empty) to `Workflow` per design.md D2; verify with a new `WorkflowTest` case: record feedback, consume it, assert the returned value matches and a second consume call returns empty.
- [x] 1.3 Add a `WorkflowTest` case asserting an empty-string feedback is accepted by `recordPendingFeedback` without error and `consumePendingFeedback()` returns an empty string (not throwing), confirming the "no specific feedback" scenario from the `stage-context-propagation` spec.

## 2. ReviewVerdict and StageOutcome

- [x] 2.1 Add `ReviewVerdict` enum (`APPROVED`, `CHANGES_REQUIRED`) to `workflow.api` per design.md D3; verify it compiles and is referenced by `StageOutcome`.
- [x] 2.2 Update `StageOutcome` to carry `Optional<ReviewVerdict> verdict` and `Optional<List<String>> findings`, keeping existing `success(stage, output)`/`failure(stage, output)` factories (defaulting both new fields to `Optional.empty()`) and adding `StageOutcome.reviewed(stage, output, verdict, findings)` per design.md D3; verify by grepping every `StageOutcome(` / `StageOutcome.success(` / `StageOutcome.failure(` usage in `src/main` and `src/test` first, then running the full existing test suite (`WorkflowEngineImplTest`, `WorkflowTest`, `AgentStageRunnerTest`) and confirming no existing construction or destructuring call site fails to compile.
- [x] 2.3 Add a `StageOutcomeTest` (new or extend an existing outcome test if one exists) asserting `success(...)`/`failure(...)` produce `Optional.empty()` for both `verdict` and `findings`, and `reviewed(...)` produces the supplied verdict and findings list.

## 3. Verdict/findings extraction in AgentStageRunner

- [x] 3.1 Implement the extraction algorithm in `AgentStageRunner` exactly as specified in design.md D4 and the `review-verdict` spec: locate zero or one ```` ```json ```` fenced block in the reviewer's entire final text (fail if more than one); parse the candidate document (the single block's content, or the whole trimmed text if no block exists) as JSON; require a JSON object with `verdict` (string, exactly `"APPROVED"` or `"CHANGES_REQUIRED"`) and `findings` (array of strings, may be empty); this step runs only for `stage == REVIEW` and only when `result.status() == PiRunStatus.SUCCESS`. Verify with new `AgentStageRunnerTest` cases: a single ```` ```json ```` block with `{"verdict":"APPROVED","findings":[]}` yields `StageOutcome.reviewed(REVIEW, _, APPROVED, [])`; a single ```` ```json ```` block with `{"verdict":"CHANGES_REQUIRED","findings":["x","y"]}` yields `StageOutcome.reviewed(REVIEW, _, CHANGES_REQUIRED, ["x","y"])`; a response with no fenced block whose entire trimmed text is a valid JSON object of the required shape also succeeds.
- [x] 3.2 Update `AgentStageRunner` so a `REVIEW` run with `PiRunResult.status() != SUCCESS` returns `StageOutcome.failure(REVIEW, <diagnostics>)` with no verdict and no findings (unchanged from today's non-REVIEW failure mapping, but verified explicitly for REVIEW); verify with an `AgentStageRunnerTest` case using a `PiRunner` stub returning `FAILURE`/`TIMEOUT` for the reviewer agent, asserting `outcome.success() == false`, `outcome.verdict().isEmpty()`, and `outcome.findings().isEmpty()`.
- [x] 3.3 Add `AgentStageRunnerTest` cases, one per extraction-failure reason defined in design.md D4 and the `review-verdict` spec, each asserting `outcome.success() == false`, `outcome.verdict().isEmpty()`, and `outcome.findings().isEmpty()`:
  - two ```` ```json ```` blocks present (ambiguous)
  - candidate document is not valid JSON
  - candidate document is valid JSON but not an object (e.g. a JSON array)
  - `verdict` key absent
  - `verdict` value is a string not equal to `"APPROVED"` or `"CHANGES_REQUIRED"` (e.g. `"approved"` lowercase, or `"MAYBE"`)
  - `verdict` value is not a string (e.g. a number)
  - `findings` key absent
  - `findings` value is not an array (e.g. a string)
  - `findings` array contains a non-string element
- [x] 3.4 Update the reviewer agent's prompt/system instructions (built in `AgentStageRunner.promptFor` for the `REVIEW` stage) to explicitly instruct the agent to emit exactly one ```` ```json ```` fenced block as the final part of its response, containing only `{"verdict": "APPROVED" | "CHANGES_REQUIRED", "findings": [...]}`, so the extraction contract in 3.1-3.3 is realistically satisfiable; verify by reading the updated prompt text and confirming it names the exact required shape (keys, allowed verdict values, fenced-block format).

## 4. StageContext passed into stage invocations

- [x] 4.1 Update `StageRunner.run(WorkflowStage, Workflow, StageContext)` port signature per design.md D1; update `AgentStageRunner` to implement the new signature.
- [x] 4.2 Update `AgentStageRunner.promptFor(...)` to build prompts from the `StageContext` (task, targetRepo, analysisFindings, proposalOutput, pendingFeedback) instead of only `workflow.getTask()`/`workflow.getTargetRepo()`, including the analysis findings and proposal output in the `IMPLEMENTATION` and `REVIEW` prompts, and the pending feedback in the `CHANGE_PROPOSAL`/`IMPLEMENTATION` prompts when present; verify with new `AgentStageRunnerTest` cases asserting the built prompt text contains the supplied findings/proposal/feedback strings when present, and omits any feedback/findings/proposal mention when absent.
- [x] 4.3 Update `WorkflowEngineImpl.advance()` to build a `StageContext` immediately before each `stageRunner.run(...)` call, reading the latest recorded `PROJECT_ANALYSIS`/`CHANGE_PROPOSAL` outcome from `workflow.outcomes()` and calling `workflow.consumePendingFeedback()` exactly once per invocation, per design.md D1/D2; verify with a `WorkflowEngineImplTest` case asserting the `StageRunner` stub receives a `StageContext` whose `analysisFindings`/`proposalOutput` match previously recorded outcomes once those stages have run.
- [x] 4.4 Add a `WorkflowEngineImplTest` case asserting the `PROJECT_ANALYSIS` stage's `StageContext` has empty `analysisFindings` and `proposalOutput` (first-run scenario from the `stage-context-propagation` spec).
- [x] 4.5 Add a `WorkflowEngineImplTest` case confirming pending feedback consumed by one stage run does not reappear in the `StageContext` of a later, unrelated stage run in the same or a later `advance()` call (the "consumed feedback does not leak" scenario).

## 5. Feedback-carrying WorkflowEngine API

- [x] 5.1 Update `WorkflowEngine.requestRevisions(WorkflowId, String feedback)` and `requestFurtherWork(WorkflowId, String feedback)` signatures per design.md D6 (**BREAKING**, no overload kept); update `WorkflowEngineImpl` to call `workflow.recordPendingFeedback(feedback)` before `advance(...)`.
- [x] 5.2 Update `FlowCommands` shell commands for revision/further-work requests to accept and pass a feedback argument (default to empty string when the user supplies none); verify by running/compiling the shell command and confirming an empty-argument invocation still succeeds.
- [x] 5.3 Add `WorkflowEngineImplTest` cases: `requestRevisions_withFeedback_shouldThreadFeedbackIntoNextProposalStage` and `requestFurtherWork_withFeedback_shouldThreadFeedbackIntoNextImplementationStage`, asserting the `StageContext` seen by the `StageRunner` stub on the next relevant invocation contains the supplied feedback text.
- [x] 5.4 Add a `WorkflowEngineImplTest` case `requestRevisions_withEmptyFeedback_shouldSucceed` asserting an empty feedback string is accepted without error and the workflow transitions exactly as it did before this change.

## 6. REVIEW routing on verdict, fail-closed

- [x] 6.1 Split `advance()`'s `REVIEW` handling into its own `case REVIEW` (out of the shared `PROJECT_ANALYSIS, CHANGE_PROPOSAL, IMPLEMENTATION, REVIEW` arm) per design.md D5: on `!outcome.success()`, call `workflow.markFailed()`; on `outcome.success()` with `verdict() == APPROVED`, `moveTo(ACCEPTANCE)`; on `outcome.success()` with `verdict() == CHANGES_REQUIRED`, `moveTo(IMPLEMENTATION)`; on `outcome.success()` with an empty verdict (defensive case), call `workflow.markFailed()` with a diagnostic naming the invariant violation.
- [x] 6.2 Add a `WorkflowEngineImplTest` case `review_approved_shouldAdvanceToAcceptance` using a `StageRunner` stub returning `StageOutcome.reviewed(REVIEW, _, APPROVED, [])`, asserting the workflow reaches `ACCEPTANCE`/`AWAITING_USER`.
- [x] 6.3 Add a `WorkflowEngineImplTest` case `review_changesRequired_shouldReturnToImplementation` using a `StageRunner` stub returning `StageOutcome.reviewed(REVIEW, _, CHANGES_REQUIRED, ["fix x"])`, asserting the workflow returns to `IMPLEMENTATION` and continues processing (matching existing correction-loop behavior).
- [x] 6.4 Add a `WorkflowEngineImplTest` case `review_technicalFailure_shouldMarkFailed_notLoop` using a `StageRunner` stub returning `StageOutcome.failure(REVIEW, _)` for the `REVIEW` stage, asserting the workflow's final status is `FAILED` (not a loop back to `IMPLEMENTATION`), confirming the proposal's declared bug fix.
- [x] 6.5 Add a `WorkflowEngineImplTest` case `review_malformedVerdict_shouldMarkFailed` using a `StageRunner` stub returning `StageOutcome.success(REVIEW, _)` (success with no verdict attached, simulating the defensive invariant-violation path), asserting the workflow's final status is `FAILED` and it does not advance to `ACCEPTANCE`.
- [x] 6.6 Confirm (by reading and, if needed, adjusting) that the existing `WorkflowEngineImplTest` review-loop/happy-path tests construct their `StageRunner` stubs using `StageOutcome.reviewed(...)` with an explicit verdict rather than the old `StageOutcome.success(REVIEW, ...)`/`failure(REVIEW, ...)` shape, updating any that do not, so no test relies on the now-removed implicit "success loops or advances" behavior for `REVIEW`.

## 7. Documentation and validation

- [x] 7.1 Update `docs/workflow.md` if it documents the `REVIEW` stage's pass/fail behavior or the `requestRevisions`/`requestFurtherWork` API, correcting it to describe the `ReviewVerdict`-based routing, the exact extraction contract, and the feedback parameter; if it does not currently describe this level of detail, add a short note instead of leaving stale wording. Verify by reading the file after edit and confirming no remaining text implies `REVIEW` success/failure alone determines routing, or implies verdict extraction is a keyword/substring scan.
- [x] 7.2 Run `openspec validate 12-add-stage-context-and-review-verdicts --strict` and fix any reported spec formatting issues; verify the command exits 0.

## 8. Boundaries with related plans (no implementation here)

- [x] 8.1 Confirm by reading plan 13 (deterministic verification) that this change's `StageContext`/`ReviewVerdict`/findings additions expose what plan 13 needs (prior proposal text; a place to plug in a richer verification result without touching `REVIEW` routing) without pre-building plan 13's logic; record any gap found as a note in this change rather than resolving it by editing plan 13's artifacts. This change does not implement anything owned by plan 13, and plan 13 implementation SHALL NOT begin until this change (plan 12) is fully implemented - no fallback path exists for plan 13 to implement this change's `StageOutcome` shape itself.
