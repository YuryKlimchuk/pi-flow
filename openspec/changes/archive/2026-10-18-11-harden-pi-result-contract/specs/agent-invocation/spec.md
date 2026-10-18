# Spec Delta

## MODIFIED Requirements

### Requirement: Invoke agent in JSON mode
The orchestrator SHALL launch the Pi CLI as a child process in JSON mode with a prompt and capture its JSONL output. A run SHALL NOT be reported as `SUCCESS` on exit code 0 alone: it SHALL also require a recognized completion signal (an `agent_settled` event) and non-blank authoritative final text, or an explicit, non-error terminal signal that the supported protocol defines as successful completion without text. Exit code 0 with an empty or entirely unrecognized/malformed event stream SHALL be reported as `FAILURE`.

#### Scenario: successful run
- **WHEN** Pi completes the run successfully, exits 0, and the JSONL stream contains an `agent_settled` event and non-blank authoritative final text
- **THEN** the runner returns the final assistant text and a SUCCESS outcome

#### Scenario: failed run
- **WHEN** Pi exits nonzero or reports a failed or aborted response
- **THEN** the runner returns a FAILURE outcome distinguishable from success

#### Scenario: empty stream with exit code 0 is not success
- **WHEN** Pi exits 0 but produced no stdout lines at all
- **THEN** the runner returns a FAILURE outcome, not SUCCESS

#### Scenario: malformed-only stream with exit code 0 is not success
- **WHEN** Pi exits 0 and every stdout line fails to parse as a recognized JSONL event
- **THEN** the runner returns a FAILURE outcome, not SUCCESS, with diagnostics identifying the stream as unparseable

#### Scenario: exit code 0 without a completion event is not success
- **WHEN** Pi exits 0 and the stdout stream ends without ever emitting an `agent_settled` event
- **THEN** the runner returns a FAILURE outcome, not SUCCESS, with diagnostics identifying the missing completion signal

### Requirement: Assemble structured result from events
The runner SHALL assemble the JSONL event stream into a structured result. The resulting diagnostics text SHALL preserve the specific error or abort reason reported by the originating event (`message_end.stopReason`, `agent_settled.aborted`) rather than being overwritten by a generic message when a specific reason is available. The runner SHALL distinguish, where the event stream demonstrates it, between a terminal error (an error or abort signal present at or after the run's final settlement, or no settlement at all) and an intermediate error that a later event in the same run shows was recovered from (an error-bearing `message_end` followed by a subsequent successful `message_end` and a non-aborted `agent_settled`); only the terminal case SHALL cause the run to be reported as FAILURE.

#### Scenario: delta assembly
- **WHEN** `message_update` delta events arrive
- **THEN** the runner accumulates them and the final text reflects the authoritative `message_end` message

#### Scenario: completion signal
- **WHEN** the `agent_settled` event arrives
- **THEN** the run is considered complete

#### Scenario: error reason is preserved into diagnostics
- **WHEN** a `message_end` event reports `stopReason: "error"`
- **THEN** the result's diagnostics text includes that specific reason rather than a generic failure message

#### Scenario: abort reason is preserved into diagnostics
- **WHEN** an `agent_settled` event reports `aborted: true`
- **THEN** the result's diagnostics text includes the abort reason rather than a generic failure message

#### Scenario: intermediate error recovered before settlement is not a terminal failure
- **WHEN** an earlier `message_end` in the run reports `stopReason: "error"`, and a later `message_end` in the same run reports a non-error stop reason followed by `agent_settled` with `aborted: false`
- **THEN** the runner reports the run as SUCCESS, reflecting the demonstrated recovery, instead of FAILURE based on the earlier error alone

#### Scenario: error at or after settlement is a terminal failure
- **WHEN** the run's final `message_end` before `agent_settled` reports `stopReason: "error"`, or `agent_settled` reports `aborted: true`, with no later successful event in the same run
- **THEN** the runner reports the run as FAILURE

## ADDED Requirements

### Requirement: Supported protocol detection
The runner SHALL determine whether the JSONL event stream is consistent with a supported Pi JSON-mode protocol shape before treating it as a basis for success. A run whose event stream does not exhibit any recognized event type SHALL be reported as FAILURE with diagnostics naming the unsupported or undetectable protocol, rather than silently falling back to best-effort text assembly treated as success.

#### Scenario: unrecognized event stream is reported as failure
- **WHEN** every line read from stdout parses as JSON but none matches a known event `type` (`message_update`, `message_end`, `agent_settled`)
- **THEN** the runner reports FAILURE with diagnostics identifying that no supported protocol event was recognized

#### Scenario: recognized event types are accepted
- **WHEN** the stream contains at least one line matching a known event `type`
- **THEN** the runner proceeds to evaluate completion and success per the other requirements in this capability

### Requirement: Distinguishable failure categories in stage outcome
The runner SHALL keep timeout, process-start failure, and protocol/content failures (empty stream, malformed-only stream, missing completion, unsupported protocol, terminal error) distinguishable from one another in the result it returns: timeouts SHALL be reported with `PiRunStatus.TIMEOUT`, and all other failures SHALL be reported with `PiRunStatus.FAILURE` accompanied by diagnostics text that identifies which category produced the failure.

#### Scenario: timeout is distinguished from other failures
- **WHEN** the Pi process does not finish within the configured timeout
- **THEN** the runner returns a TIMEOUT outcome, not a generic FAILURE

#### Scenario: process start failure is identified in diagnostics
- **WHEN** the Pi process fails to start
- **THEN** the runner returns a FAILURE outcome whose diagnostics identify that the process could not be started, distinct from a protocol or content failure

#### Scenario: protocol/content failure is identified in diagnostics
- **WHEN** the run fails because of an empty stream, a malformed-only stream, a missing completion event, or an unsupported protocol
- **THEN** the runner returns a FAILURE outcome whose diagnostics identify which of those conditions occurred

### Requirement: Reader lifecycle and cleanup robustness
The runner SHALL deterministically stop and join its stdout and stderr reader threads, and remove any temporary prompt or skill files it created, on every exit path (normal completion, failure, timeout, interrupted wait, and process-start failure). Output accumulated by a reader thread after the process has exited or been destroyed SHALL NOT be used to upgrade a run that is otherwise a failure (empty, malformed-only, missing completion, or terminal error) into a SUCCESS outcome merely because some text is present.

#### Scenario: reader threads are joined on timeout
- **WHEN** a run times out and the process is forcibly destroyed
- **THEN** the runner joins the stdout and stderr reader threads before returning the TIMEOUT result

#### Scenario: temp files are removed after a failed run
- **WHEN** an agent run creates a temporary system-prompt or skill file and the run fails
- **THEN** the temporary files are removed before the runner returns the result

#### Scenario: late text after process exit does not flip a failure to success
- **WHEN** the process exits in a way that would otherwise be reported as FAILURE (for example, no `agent_settled` event was observed), and the stdout reader thread had accumulated some streamed text before exit
- **THEN** the runner still reports FAILURE; the presence of streamed text alone does not change the outcome
