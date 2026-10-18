# controlled-stage-retry Specification

## Purpose

Defines the `PAUSED` workflow status for retryable technical failures, which stages are retry-eligible and under what condition (including the explicit acknowledgement required for a write stage, which includes `CHANGE_PROPOSAL`), the distinction between a technical-failure retry and a content-correction loop (including the verifier exit-code-vs-timeout split), the preserved attempt history, and the configurable, bounded retry/correction budgets that stop either kind from repeating indefinitely.

## Requirements

### Requirement: PAUSED is a distinct, non-terminal status for retryable technical failures
The orchestrator SHALL support a `PAUSED` workflow status, distinct from `AWAITING_USER`. `AWAITING_USER` SHALL be used only for an ordinary gate awaiting a human decision (`VALIDATION_APPROVAL`, `ACCEPTANCE`, and the `NOT_CONFIGURED` verification pause). `PAUSED` SHALL be used only for a workflow stopped by a technical failure of an eligible stage while that stage-attempt-sequence's technical-retry budget has at least one attempt remaining. `PAUSED` SHALL NOT be a terminal status.

#### Scenario: technical failure with budget remaining pauses, not fails
- **WHEN** an eligible stage's execution fails technically (process crash, timeout, or malformed/unparseable output) and the stage's technical-retry counter has not yet reached the configured budget
- **THEN** the orchestrator transitions the workflow to `PAUSED`, not `FAILED`, and not `AWAITING_USER`

#### Scenario: ordinary gates remain AWAITING_USER
- **WHEN** a workflow reaches `VALIDATION_APPROVAL` or `ACCEPTANCE`
- **THEN** the orchestrator reports the workflow's status as `AWAITING_USER`, not `PAUSED`

#### Scenario: PAUSED is not terminal
- **WHEN** a workflow's status is `PAUSED`
- **THEN** the orchestrator does not treat the workflow as terminal, and a subsequent eligible retry request may act on it

### Requirement: Budget exhaustion, not routing to PAUSED, is what produces FAILED
A technical failure that would exceed the stage's technical-retry budget - either because this is the attempt that consumes the last remaining retry and still fails, or because a retry is requested when the budget is already exhausted - SHALL mark the workflow `FAILED` rather than `PAUSED`. Once `FAILED`, the workflow SHALL remain terminal and immutable per the existing terminal-state guarantee; no further retry of any kind SHALL be accepted for it.

#### Scenario: the exhausting attempt marks the workflow failed
- **WHEN** an eligible stage's technical-retry counter is one below the configured budget and a further attempt also fails technically
- **THEN** the orchestrator marks the workflow `FAILED`, not `PAUSED`

#### Scenario: FAILED remains terminal after exhaustion
- **WHEN** a workflow has been marked `FAILED` due to technical-retry budget exhaustion
- **THEN** a further retry request against that workflow is rejected and the workflow's status remains `FAILED`

### Requirement: Retry eligibility is scoped by whether a stage writes to the target repository
`PROJECT_ANALYSIS`, `REVIEW`, and `VERIFICATION` SHALL be retry-eligible for a technical failure with no additional acknowledgement required beyond identifying the workflow. `CHANGE_PROPOSAL` and `IMPLEMENTATION` SHALL both be treated as write stages, since each may create or partially modify artifact files - OpenSpec change artifacts for `CHANGE_PROPOSAL`, the target repository's source files for `IMPLEMENTATION` - before failing; each SHALL be retry-eligible only when the retry request includes an explicit acknowledgement that partial changes may be present (an `--acknowledge-partial-changes` flag or equivalent). A retry request for `CHANGE_PROPOSAL` or `IMPLEMENTATION` without that acknowledgement SHALL be rejected and SHALL leave the workflow's status, stage, and both retry/correction counters unchanged. The orchestrator SHALL NOT, on its own initiative and without an explicit retry request, re-run `CHANGE_PROPOSAL`, `IMPLEMENTATION`, or any other write stage as a technical-failure retry; this prohibition governs only the technical-failure retry mechanism and does NOT prohibit the orchestrator's existing, separate content-correction loop (for example `REVIEW`'s `CHANGES_REQUIRED` verdict routing back to `IMPLEMENTATION`, or a revision request routing back to `CHANGE_PROPOSAL`), which is normal control flow for a validly-executed stage reporting a content problem, not a technical-failure retry, and remains governed by the content-correction budget instead.

#### Scenario: non-write stage retried without any special flag
- **WHEN** the user requests a retry while the workflow is `PAUSED` at `REVIEW` after a technical failure
- **THEN** the orchestrator re-runs `REVIEW` without requiring any acknowledgement flag

#### Scenario: write stage retry rejected without acknowledgement
- **WHEN** the user requests a retry while the workflow is `PAUSED` after `IMPLEMENTATION` failed technically, without passing the required acknowledgement
- **THEN** the orchestrator rejects the retry request and does not re-run `IMPLEMENTATION`, and the workflow's status, stage, and counters remain unchanged

#### Scenario: write stage retry accepted with explicit acknowledgement
- **WHEN** the user requests a retry while the workflow is `PAUSED` after `IMPLEMENTATION` failed technically, passing the required acknowledgement that partial changes may be present
- **THEN** the orchestrator re-runs `IMPLEMENTATION`, subject to the same technical-retry budget check as any other eligible stage

#### Scenario: CHANGE_PROPOSAL retry rejected without acknowledgement
- **WHEN** the user requests a retry while the workflow is `PAUSED` after `CHANGE_PROPOSAL` failed technically, without passing the required acknowledgement
- **THEN** the orchestrator rejects the retry request and does not re-run `CHANGE_PROPOSAL`, and the workflow's status, stage, and counters remain unchanged

#### Scenario: CHANGE_PROPOSAL retry accepted with explicit acknowledgement
- **WHEN** the user requests a retry while the workflow is `PAUSED` after `CHANGE_PROPOSAL` failed technically, passing the required acknowledgement that partial changes may be present
- **THEN** the orchestrator re-runs `CHANGE_PROPOSAL`, subject to the same technical-retry budget check as any other eligible stage

#### Scenario: no automatic technical-failure retry of a write stage
- **WHEN** `CHANGE_PROPOSAL` or `IMPLEMENTATION` fails technically and no explicit retry request has been made
- **THEN** the orchestrator does not re-run that stage on its own as a technical-failure retry

#### Scenario: the content-correction loop is not blocked by the write-stage retry prohibition
- **WHEN** the `REVIEW` stage executes validly and reports a `ReviewVerdict` of `CHANGES_REQUIRED`
- **THEN** the orchestrator routes the workflow back to `IMPLEMENTATION` as a content correction, without requiring `--acknowledge-partial-changes` and without this being treated as a prohibited automatic retry of a write stage

#### Scenario: retry rejected for a terminal workflow
- **WHEN** the user requests a retry on a workflow whose status is `COMPLETED`, `CANCELLED`, or `FAILED`
- **THEN** the orchestrator rejects the retry request and the workflow's status and stage remain unchanged

### Requirement: Technical failures and content corrections are distinguishable, including for verification
The orchestrator SHALL classify every stage attempt as exactly one of `INITIAL`, `TECHNICAL_RETRY` (the stage's prior execution did not complete validly - a process crash, timeout, or malformed/unparseable agent output), or `CONTENT_CORRECTION` (the stage's prior execution completed validly but its result required another pass). For the automated verification check specifically, the orchestrator SHALL further distinguish, within a non-passing result, a **nonzero exit code from a command that started and completed within its configured timeout** (classified as a content correction) from a **failure to start the configured command's process, or a timeout before it completed** (classified as a technical failure). A technical failure SHALL NOT be recorded or reported as a content correction, and a content correction SHALL NOT be recorded or reported as a technical failure.

#### Scenario: crashed stage is classified as a technical failure
- **WHEN** a stage's execution fails because of a process crash, timeout, or malformed agent output
- **THEN** the orchestrator classifies the resulting attempt as a technical failure, not a content correction

#### Scenario: changes-required review is classified as a content correction
- **WHEN** the `REVIEW` stage executes successfully and reports changes required
- **THEN** the orchestrator classifies the resulting loop as a content correction, not a technical failure

#### Scenario: verification command running to completion with a nonzero exit is a content correction
- **WHEN** the automated verification command starts, completes within its configured timeout, and exits with a nonzero status
- **THEN** the orchestrator classifies the resulting loop as a content correction and routes the workflow back to `IMPLEMENTATION`

#### Scenario: verification command failing to start or timing out is a technical failure
- **WHEN** the automated verification command fails to start, or does not complete within its configured timeout
- **THEN** the orchestrator classifies the resulting failure as a technical failure and, if budget remains, transitions the workflow to `PAUSED` rather than routing it to `IMPLEMENTATION` as a content correction

### Requirement: Full attempt history is preserved and classified across retries
Every attempt - initial, technical-failure retry, or content correction - SHALL be recorded as part of the workflow's stage outcome history without overwriting or discarding any prior attempt's record. The complete sequence of attempts for a stage SHALL remain inspectable after any number of retries, and each recorded attempt SHALL identify which of `INITIAL`, `TECHNICAL_RETRY`, or `CONTENT_CORRECTION` it was.

#### Scenario: multiple retry attempts are all recorded
- **WHEN** a stage is retried more than once before succeeding
- **THEN** the workflow's recorded history includes every attempt, in order, not only the final successful one

#### Scenario: attempt kind is visible in the recorded history
- **WHEN** a stage's history includes both a technical-failure retry and a content-correction loop
- **THEN** each recorded attempt in the history identifies which kind it was

### Requirement: Retry and correction budgets are configurable, bounded, and independently tracked, with explicit defaults
The orchestrator SHALL enforce a configurable maximum number of technical-failure retries per stage-attempt-sequence (default: 3) and a configurable maximum number of content-correction loops per workflow (default: 5), each with its default applied when not otherwise configured. The technical-retry counter SHALL be scoped per stage-attempt-sequence and SHALL reset when the workflow advances to a new stage via a successful outcome. The content-correction counter SHALL be scoped per workflow and SHALL NOT reset for the lifetime of the workflow. Exceeding either configured budget SHALL mark the workflow `FAILED` instead of allowing further retry or looping of that budget's kind. A retry request rejected for any reason other than a successful or failed attempt actually being dispatched (missing required acknowledgement, workflow not in a retryable state, stage not eligible, workflow terminal) SHALL NOT increment either counter.

#### Scenario: retry succeeds within the technical-failure budget
- **WHEN** a stage experiences a technical failure and is retried a number of times at or below the configured technical-failure retry budget
- **THEN** the orchestrator allows each retry attempt and increments the technical-retry counter for each one actually dispatched

#### Scenario: technical-failure budget exhaustion marks the workflow failed
- **WHEN** a stage's technical-failure retries reach the configured budget without a valid execution
- **THEN** the orchestrator marks the workflow `FAILED` and does not allow a further technical-failure retry for that stage-attempt-sequence

#### Scenario: content-correction loop proceeds within budget
- **WHEN** a content-correction loop (`REVIEW` changes-required, or verification exit-code failure) repeats a number of times at or below the configured content-correction budget
- **THEN** the orchestrator allows the workflow to continue looping through implementation and the corrected stage

#### Scenario: content-correction budget exhaustion marks the workflow failed
- **WHEN** a workflow's content-correction loop count reaches the configured budget without reaching a passing result
- **THEN** the orchestrator marks the workflow `FAILED` and does not allow a further content-correction loop

#### Scenario: default budgets apply when not configured
- **WHEN** no explicit technical-failure retry budget or content-correction budget is configured
- **THEN** the orchestrator applies its documented default values (3 and 5, respectively) for each budget

#### Scenario: technical-retry counter resets on successful stage advancement
- **WHEN** a stage that previously failed technically is retried and succeeds, and the workflow advances to the next stage
- **THEN** the technical-retry counter for the new stage's attempt sequence starts at zero

#### Scenario: a rejected retry request does not consume budget
- **WHEN** a retry request is rejected because required acknowledgement is missing, the workflow is not in a retryable state, the stage is not eligible, or the workflow is terminal
- **THEN** neither the technical-retry counter nor the content-correction counter changes as a result of that request

### Requirement: Auto-retry is disabled by default with no automatic re-invocation of a failed stage
The orchestrator SHALL NOT automatically re-invoke a stage that failed technically. The only mechanisms that re-run a stage SHALL be: the orchestrator's own content-correction loop for a validly-executed stage reporting a non-passing content result, and an explicit, manually-invoked retry request. No default-enabled or implicitly-enabled configuration SHALL cause a technically-failed stage to be re-run without an explicit retry request.

#### Scenario: a technical failure does not trigger an automatic retry
- **WHEN** an eligible stage fails technically and no explicit retry request is made
- **THEN** the orchestrator leaves the workflow `PAUSED` (or `FAILED`, if budget is exhausted) and does not re-run the stage on its own

#### Scenario: only an explicit retry request re-runs a technically-failed stage
- **WHEN** the user issues an explicit retry request for a `PAUSED` workflow at an eligible stage with budget remaining
- **THEN** the orchestrator re-runs that stage as a direct result of the request, not as a result of any automatic mechanism

### Requirement: A single general retry command serves both technical-failure retry and the NOT_CONFIGURED verification pause
The orchestrator SHALL expose exactly one retry command usable both for a workflow `PAUSED` at an eligible stage and for a workflow `AWAITING_USER` at the `VERIFICATION` stage's `NOT_CONFIGURED` pause. Invoking the command against the `NOT_CONFIGURED` pause SHALL re-run the existing verification configuration re-check path and then resume normal advancement; no second, separate retry command SHALL exist for this case.

#### Scenario: the general retry command resolves a NOT_CONFIGURED pause
- **WHEN** the user invokes the retry command against a workflow `AWAITING_USER` at `VERIFICATION` with a `NOT_CONFIGURED` result, after verification configuration has been corrected
- **THEN** the orchestrator re-checks verification configuration and resumes advancement using the same command used for a `PAUSED` technical-retry

#### Scenario: no duplicate retry command exists
- **WHEN** the retry capability for `NOT_CONFIGURED` is inspected
- **THEN** it is served by the same command as every other retry-eligible case, with no separate, stage-specific retry command remaining
