# Spec Delta

## MODIFIED Requirements

### Requirement: Correction loop
A `FAILED` verification result caused by a command that started and completed within its timeout but exited with a nonzero status, and a `REVIEW` outcome carrying a `ReviewVerdict` of `CHANGES_REQUIRED`, SHALL return the workflow to implementation as content corrections. A technical failure of the `REVIEW` stage's execution, and a malformed/unparseable review verdict, SHALL NOT return the workflow to implementation and SHALL NOT be marked `FAILED` directly; each SHALL be handled per the `controlled-stage-retry` capability's technical-failure requirements, pausing the workflow at `PAUSED` while the `REVIEW` stage-attempt-sequence's technical-retry budget has at least one attempt remaining and marking the workflow `FAILED` once that budget is exhausted. A `NOT_CONFIGURED` verification result SHALL NOT be treated as a correction and SHALL NOT return the workflow to implementation. A verification failure caused by the configured command failing to start, or by the command timing out before completion, SHALL NOT be treated as a content correction either; it SHALL be handled per the `controlled-stage-retry` capability's technical-failure/`PAUSED` requirements instead of looping to `IMPLEMENTATION`. The number of times a content-correction loop may repeat for a single workflow SHALL be bounded by a configurable content-correction budget, defined and defaulted by the `controlled-stage-retry` capability; reaching that budget SHALL mark the workflow `FAILED` instead of looping further.

#### Scenario: verification failed
- **WHEN** the verification result is `FAILED` because the configured command started, completed within its timeout, and exited with a nonzero status
- **THEN** the workflow returns to implementation

#### Scenario: review requires corrections
- **WHEN** the `REVIEW` stage outcome carries a `ReviewVerdict` of `CHANGES_REQUIRED`
- **THEN** the workflow returns to implementation

#### Scenario: stage execution raises an exception
- **WHEN** the stage runner throws an exception while executing an agent stage
- **THEN** the orchestrator records a failed stage outcome describing the exception, pauses the workflow at `PAUSED` while the stage-attempt-sequence's technical-retry budget has at least one attempt remaining, and marks the workflow `FAILED` once that budget is exhausted, instead of propagating the exception

#### Scenario: verification raises an exception
- **WHEN** the verifier throws an exception during the verification stage
- **THEN** the orchestrator records a failed stage outcome describing the exception, pauses the workflow at `PAUSED` while the `VERIFICATION` stage-attempt-sequence's technical-retry budget has at least one attempt remaining, and marks the workflow `FAILED` once that budget is exhausted, instead of propagating the exception

#### Scenario: fatal JVM errors are not caught
- **WHEN** the stage runner or verifier throws an `Error` that represents a fatal JVM condition (for example `OutOfMemoryError` or `StackOverflowError`)
- **THEN** the orchestrator does not catch it as a failed stage outcome and lets it propagate

#### Scenario: review technical failure pauses the workflow instead of looping
- **WHEN** the `REVIEW` stage's execution fails for a technical reason (process crash, timeout, or a failed agent run)
- **THEN** the orchestrator pauses the workflow at `PAUSED` while the `REVIEW` stage-attempt-sequence's technical-retry budget has at least one attempt remaining, and marks the workflow `FAILED` once that budget is exhausted, instead of returning it to `IMPLEMENTATION`

#### Scenario: failed verdict extraction pauses the workflow instead of looping
- **WHEN** the `REVIEW` stage executes without a technical error but its output cannot be parsed into a valid `ReviewVerdict`
- **THEN** the orchestrator pauses the workflow at `PAUSED` while the `REVIEW` stage-attempt-sequence's technical-retry budget has at least one attempt remaining, and marks the workflow `FAILED` once that budget is exhausted, instead of returning it to `IMPLEMENTATION` or advancing to `ACCEPTANCE`

#### Scenario: unconfigured verification does not trigger a correction loop
- **WHEN** the verification result is `NOT_CONFIGURED`
- **THEN** the orchestrator does not return the workflow to `IMPLEMENTATION` as if a correction were requested

#### Scenario: a verification infrastructure error does not trigger a correction loop
- **WHEN** the verification result is `INFRASTRUCTURE_ERROR`
- **THEN** the orchestrator does not return the workflow to `IMPLEMENTATION` as if a correction were requested, and does not treat the result as equivalent to `FAILED`

#### Scenario: verification process-start or timeout failure does not trigger a correction loop
- **WHEN** the configured verification command fails to start, or does not complete within its configured timeout
- **THEN** the orchestrator does not return the workflow to `IMPLEMENTATION` as a content correction; it handles the failure as a technical failure instead

#### Scenario: correction loop budget exhaustion stops further looping
- **WHEN** the number of correction loops for a workflow reaches the configured content-correction budget without reaching a passing result
- **THEN** the orchestrator marks the workflow `FAILED` and does not return it to implementation again

### Requirement: Unconfigured or broken verification configuration blocks automatic progression with an explicit, recoverable state
When the verification result is `NOT_CONFIGURED`, the orchestrator SHALL NOT advance the workflow to `REVIEW` and SHALL NOT return it to `IMPLEMENTATION` as a correction; it SHALL instead pause the workflow at `AWAITING_USER` at the `VERIFICATION` stage, naming the missing configuration file as the cause, recoverable by the general retry command with no technical-retry budget consumed (unchanged from plan 13's original behavior). When the verification result is `INFRASTRUCTURE_ERROR` (which, per the corrected `deterministic-verification` classification, includes a malformed/unreadable configuration file, a command that cannot be started, and a command that is started but does not complete within its configured timeout), the orchestrator SHALL instead treat it as a technical failure of the `VERIFICATION` stage: it SHALL pause the workflow at `PAUSED`, not `AWAITING_USER`, consuming the `VERIFICATION` stage-attempt-sequence's technical-retry budget exactly as any other technical failure does, and SHALL mark the workflow `FAILED` instead of `PAUSED` once that budget is exhausted. This supersedes plan 13's original routing of `INFRASTRUCTURE_ERROR` to the same `AWAITING_USER` pause as `NOT_CONFIGURED`; `NOT_CONFIGURED`'s routing is otherwise unchanged. Recovery from either pause SHALL use the same general retry command (`controlled-stage-retry`'s single retry command requirement); for `INFRASTRUCTURE_ERROR` specifically, that command's technical-retry path (not a special-cased configuration-reload path) applies, while still re-reading the configuration file fresh per plan 13's existing fresh-read requirement.

#### Scenario: NOT_CONFIGURED still pauses at AWAITING_USER
- **WHEN** the verification result is `NOT_CONFIGURED`
- **THEN** the orchestrator pauses the workflow at `AWAITING_USER` at the `VERIFICATION` stage, not `PAUSED`, and does not consume any technical-retry budget

#### Scenario: INFRASTRUCTURE_ERROR pauses at PAUSED and consumes technical-retry budget
- **WHEN** the verification result is `INFRASTRUCTURE_ERROR` and the `VERIFICATION` stage-attempt-sequence's technical-retry counter has not yet reached the configured budget
- **THEN** the orchestrator pauses the workflow at `PAUSED`, not `AWAITING_USER`, increments the technical-retry counter, and records the attempt as `TECHNICAL_RETRY`

#### Scenario: INFRASTRUCTURE_ERROR budget exhaustion marks the workflow FAILED
- **WHEN** the verification result is `INFRASTRUCTURE_ERROR` and the `VERIFICATION` stage-attempt-sequence's technical-retry counter has already reached the configured budget
- **THEN** the orchestrator marks the workflow `FAILED` instead of pausing it again

#### Scenario: a verification timeout is classified as INFRASTRUCTURE_ERROR and therefore pauses at PAUSED
- **WHEN** the configured verification command is started but does not complete within its configured timeout
- **THEN** the verifier reports `INFRASTRUCTURE_ERROR` (per the corrected `deterministic-verification` classification) and the orchestrator pauses the workflow at `PAUSED` under the technical-retry budget, not at `AWAITING_USER`

#### Scenario: unconfigured verification pauses the workflow with a named cause
- **WHEN** the verification result is `NOT_CONFIGURED`
- **THEN** the workflow stops advancing automatically and its state identifies the missing verification configuration file as the cause

#### Scenario: a verification infrastructure error pauses the workflow with a named cause
- **WHEN** the verification result is `INFRASTRUCTURE_ERROR`
- **THEN** the workflow stops advancing automatically and its state identifies the specific infrastructure problem (for example a malformed configuration file, a missing executable, or a missing working directory) as the cause

#### Scenario: a paused-for-configuration workflow is not a terminal, stuck workflow
- **WHEN** the verification result is `NOT_CONFIGURED` or `INFRASTRUCTURE_ERROR`
- **THEN** the workflow's status is not `COMPLETED`, `CANCELLED`, or `FAILED`

#### Scenario: workflow recovers after verification configuration is fixed and retried, with no restart
- **WHEN** a workflow is paused because verification was `NOT_CONFIGURED` or `INFRASTRUCTURE_ERROR`, a valid verification configuration file is subsequently created or corrected for its target repository while the application keeps running, and the user retries verification
- **THEN** the orchestrator re-reads the configuration file, re-runs verification, and routes the new result (`PASSED`, `FAILED`, or still `NOT_CONFIGURED`/`INFRASTRUCTURE_ERROR` if the underlying cause was not actually fixed) per the other requirements in this capability, with no restart of the application
