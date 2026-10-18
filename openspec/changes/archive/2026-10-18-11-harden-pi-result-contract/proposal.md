# Proposal

## Why

`PiJsonlParser` and `JsonModePiRunner` currently infer success too optimistically: `toResult()` treats any run with exit code 0 and no explicit `error`/`aborted` signal as `SUCCESS`, even if the stdout stream was empty, consisted only of malformed JSONL lines, or never produced an `agent_settled` event. The parser also silently drops unparseable lines (`JacksonException` -> skip) with no record that a line was dropped, and does not check whether the JSONL came from a Pi version whose event shape it understands. There is also no distinction in `PiRunResult`/`PiRunStatus` between a terminal failure and a recoverable intermediate error the agent itself resolved before `agent_settled` - everything that ever sets `failed` is treated as a hard failure, which does not reflect what the JSON event protocol actually distinguishes (e.g. `message_end` with `stopReason: "error"` on one message, followed by a healthy later message before settlement). Diagnostics text is reused across distinct causes (timeout, process start failure, malformed/empty protocol stream) without a way for callers to tell them apart. These gaps let a successful-looking but empty/garbled Pi run flow into the workflow as a `SUCCESS` `StageOutcome`, which is unsafe for a human-gated workflow that relies on `StageOutcome.success()` to decide whether to advance.

## What Changes

- **BREAKING**: `PiJsonlParser`/`toResult()` SHALL NOT report `SUCCESS` for exit code 0 when the event stream is empty, contains no recognized completion event (`agent_settled`), or never produced non-blank authoritative final text under a stop reason that indicates completion. Exit 0 plus empty/unrecognized output becomes `FAILURE`, not `SUCCESS`.
- The parser SHALL track and expose whether it saw a supported Pi protocol marker/shape; runs using an unsupported or undetectable protocol version SHALL surface as `FAILURE` with a diagnostic naming the issue, rather than silently falling back to best-effort text assembly treated as success.
- `errorMessage`/failure reason text SHALL be preserved end-to-end from the originating JSONL event (`stopReason`, `agent_settled.aborted`) through `PiJsonlParser.failureReason()` into `PiRunResult.diagnostics()`, without being overwritten by generic text when a specific reason is available.
- Introduce a documented, narrow distinction between an intermediate recoverable error (an error-bearing `message_end` later followed by a successful settlement, where the protocol demonstrates recovery) and a terminal error (failure present at or after `agent_settled`, or no settlement at all). Only apply this distinction where the JSON event protocol actually demonstrates the recovery (a later healthy `message_end`/successful settlement exists in the same run); do not infer recovery from absence of further errors alone.
- `JsonModePiRunner` SHALL keep timeout, process-start failure, and malformed/empty-protocol failures distinguishable in the `StageOutcome`/`PiRunResult` the caller receives: `PiRunStatus` (`TIMEOUT` vs `FAILURE`) is preserved, and `diagnostics()` text SHALL identify which category produced the failure instead of a one-size-fits-all message.
- Harden reader-thread lifecycle and cleanup: stdout/stderr reader threads and temp files (prompt/skill) SHALL be cleaned up deterministically on every exit path (success, failure, timeout, interrupted, process-start failure), and a reader-thread failure or late-arriving output after process exit SHALL NOT be allowed to flip a failed/empty run into `SUCCESS` merely because some text happened to accumulate.
- Add negative tests: empty stdout with exit 0, malformed-only JSONL with exit 0, truncated stream missing `agent_settled`, intermediate-error-then-recovery, terminal error at settlement, and reader-thread/timeout cleanup checks.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `agent-invocation`: "Invoke agent in JSON mode" and "Assemble structured result from events" requirements are tightened so completion/failure is judged against a supported protocol contract, not exit code and event absence alone; diagnostics text and the intermediate-vs-terminal error distinction are made explicit.

## Impact

- `src/main/java/.../agent/infrastructure/PiJsonlParser.java`: completion/protocol-support tracking, no-silent-success-on-empty-stream logic, preserved failure reason text, intermediate-vs-terminal error distinction.
- `src/main/java/.../agent/infrastructure/JsonModePiRunner.java`: `toResult()` and reader-thread/cleanup paths updated to honor the hardened contract; diagnostic text differentiates timeout/process-start/protocol failures.
- `src/main/java/.../agent/api/PiRunResult.java`, `PiRunStatus.java`: reviewed for whether the intermediate-vs-terminal distinction needs a new status/field (decided in design.md); no change to `PiRunner` port signature.
- Tests: `PiJsonlParserTest`, `JsonModePiRunnerTest` gain negative/edge cases; `fake-pi` test fixture may gain new scripted scenarios.
- No live/paid Pi or LLM calls are used; all new tests run against local fake executables and fixture JSONL, consistent with existing tests.
- No changes to `workflow-orchestration` capability or shell command signatures.
