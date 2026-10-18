# Tasks

## 1. Parser: recognized-protocol and completion tracking

- [x] 1.1 Add a `sawRecognizedEvent` flag to `PiJsonlParser`, set `true` in `accept()` whenever the parsed `type` matches `message_update`, `message_end`, or `agent_settled` (not the `default` branch); expose it via a new `supportedProtocolObserved()` method; verify with a new `PiJsonlParserTest` case feeding only unrecognized/garbage `type` values and asserting `supportedProtocolObserved()` is `false`, plus a case asserting it is `true` after at least one recognized event.
- [x] 1.2 Add a `hasRecognizedCompletion()` method to `PiJsonlParser` that is `true` once `agent_settled` has been observed; verify with a `PiJsonlParserTest` case asserting it is `false` before any `agent_settled` line and `true` after one arrives.
- [x] 1.3 Update `JsonModePiRunner.toResult()` so `PiRunStatus.SUCCESS` additionally requires `parser.hasRecognizedCompletion()` and non-blank `parser.finalText()`, in addition to today's `exitCode == 0 && !parser.failed()`; verify with new `JsonModePiRunnerTest`/`PiJsonlParserTest`-level cases: exit 0 with zero stdout lines -> FAILURE; exit 0 with only malformed JSONL lines -> FAILURE; exit 0 with text deltas but no `agent_settled` -> FAILURE.
- [x] 1.4 Add a diagnostics message for each new failure path in 1.3 identifying the specific cause (empty stream vs malformed-only vs missing-completion vs unsupported-protocol), surfaced through `PiRunResult.diagnostics()`; verify with assertions that `diagnostics()` contains a distinct, path-specific phrase for each of the four cases.
- [x] 1.5 Confirm the existing passing-case tests in `JsonModePiRunnerTest` (`run_withSuccessfulPrompt_shouldReturnSuccessAndAuthoritativeText`, `run_withAgent_shouldPassAgentFlags`, `run_withAgentWithSkill_shouldPassSkill`) and `PiJsonlParserTest` (`accept_withMessageEnd_shouldUseAuthoritativeText`, `accept_withAgentSettled_shouldMarkCompletion`) still pass unchanged against the stricter rule, updating the `fake-pi` fixture or agent-pi test scripts only if they do not already emit `agent_settled` with non-blank text (confirmed by reading `src/test/resources/fake-pi` before editing).

## 2. Preserve specific failure reasons

- [x] 2.1 Verify (and adjust if needed) that `PiJsonlParser.handleMessageEnd`/`handleSettled` continue to set `failureReason` to the specific `stopReason`/`"aborted"` value, and that `JsonModePiRunner.toResult()`'s existing precedence (`stderr` first, `parser.failureReason()` as fallback) is preserved by the changes in Task 1; add a `PiJsonlParserTest`/`JsonModePiRunnerTest` case asserting `diagnostics()` equals or contains the specific `stopReason` text (e.g. `"error"`) when stderr is empty, covering both the `message_end` error case and the `agent_settled` aborted case.
- [x] 2.2 Add a `PiJsonlParserTest` case asserting `failureReason()` is not silently replaced by a generic string when a specific reason was already recorded from an earlier event in the same run.

## 3. Intermediate-vs-terminal error distinction

- [x] 3.1 Update `PiJsonlParser.handleMessageEnd`/`handleSettled` so that an error-bearing `message_end` sets `failed = true` as today, but a subsequent `message_end` with a non-error stop reason followed by `agent_settled` with `aborted: false` clears `failed` back to `false` (recovery), while a `message_end` error or `agent_settled.aborted == true` at or after the run's final settlement leaves `failed` as `true` (terminal); verify with a new `PiJsonlParserTest` case `accept_withErrorThenRecoveredSettlement_shouldClearFailed` (error `message_end`, then success `message_end`, then `agent_settled{aborted:false}` -> `failed()` is `false`).
- [x] 3.2 Add a `PiJsonlParserTest` case `accept_withErrorAtFinalSettlement_shouldStayFailed` asserting a `message_end` error immediately followed by `agent_settled{aborted:false}` (no later successful message) still reports `failed() == true`, confirming the narrow recovery rule only applies when a later successful event genuinely exists.
- [x] 3.3 Add a `JsonModePiRunnerTest` end-to-end case using a scripted fake-pi executable that emits an error `message_end`, then a successful `message_end`, then `agent_settled{aborted:false}`, asserting the overall `PiRunResult.status()` is `SUCCESS`.

## 4. Distinguishable failure categories in the runner

- [x] 4.1 Confirm `JsonModePiRunner.runCommand()` still returns `PiRunStatus.TIMEOUT` (not `FAILURE`) for the timeout path after the Task 1 changes; verify `JsonModePiRunnerTest.run_withHangingPrompt_shouldReturnTimeout` still passes unchanged.
- [x] 4.2 Add a diagnostics phrase identifying process-start failure in the `IOException` catch block of `runCommand()` (already present as `"Failed to start Pi: " + e.getMessage()`); add a `JsonModePiRunnerTest` case using a nonexistent command path, asserting `status() == FAILURE` and `diagnostics()` identifies a process-start failure distinct from the Task 1 protocol-failure phrases.
- [x] 4.3 Add a `JsonModePiRunnerTest` case asserting that for the same failing run, `diagnostics()` text differs identifiably between a timeout, a process-start failure, and a protocol/content failure (e.g. three assertions in one test or three focused tests cross-checking the phrases from 1.4 and 4.2 are mutually distinct substrings).

## 5. Reader lifecycle and late-text safeguard

- [x] 5.1 Add a `JsonModePiRunnerTest` case confirming stdout/stderr reader threads are joined before `TIMEOUT` is returned (e.g. by asserting no stray thread named `pi-stdout`/`pi-stderr` from that run remains alive shortly after the call returns, using `Thread.getAllStackTraces()` or an equivalent check scoped to the test).
- [x] 5.2 Add a `JsonModePiRunnerTest` case confirming temporary prompt/skill files created for an agent run are deleted after a run that fails (e.g. the `fail now`/error-producing fake-pi script under an agent invocation), by asserting the temp files recorded during the run no longer exist once `run()` returns.
- [x] 5.3 Add a `JsonModePiRunnerTest` case using a fake-pi script that prints stray `text_delta` events (accumulating non-blank `streamedText`) but exits without ever emitting `agent_settled`; assert the overall result is `FAILURE` despite non-blank intermediate text, confirming late/incomplete streamed text cannot flip the outcome to `SUCCESS`.

## 6. Spec and documentation alignment

- [x] 6.1 Update `docs/system-overview.md` or `docs/workflow.md` if either currently describes agent-run success as exit-code-based, correcting the description to match the stricter completion-based contract; if neither file documents this level of detail, skip this task and note the skip in the task's commit/summary rather than inventing new documentation scope.
  - **Note:** Skipped. Neither `docs/system-overview.md` nor `docs/workflow.md` describes agent-run success as exit-code-based; both only state that the orchestrator consumes the JSONL event stream to capture structured stage outcomes. No documentation edit required.
- [x] 6.2 Run `openspec validate 11-harden-pi-result-contract --strict` and fix any reported spec formatting issues; verify the command exits 0.

## 7. Boundaries with related plans (no implementation here)

- [x] 7.1 Confirm by reading plans 12-17 (context/verdict handling, verification, binding, retries, diagnostics, CLI tests) that none of this change's tasks duplicate work explicitly scoped to those plans; record any overlap found as a note in this change rather than resolving it by editing another plan's artifacts.
  - **Note:** No overlap found. Plan 16 (`16-add-workflow-diagnostics`) consumes this change's `PiRunResult.diagnostics()` timeout/process-start/malformed-protocol text as an input for its own diagnostic reason strings, but it does not re-scope or duplicate the protocol-failure detection implemented here. No plan 12-17 artifacts were edited.
