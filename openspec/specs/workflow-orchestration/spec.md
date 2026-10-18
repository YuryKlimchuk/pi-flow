# workflow-orchestration Specification

## Purpose
Orchestrates the pi-flow development workflow as a synchronous state machine over stages, approval gates, and correction loops, with persisted workflow state.

## Requirements

### Requirement: Start workflow
Given a task description and a target repository path, the orchestrator SHALL create a workflow and persist its initial state only after the target repository path is confirmed, via `git rev-parse`, to resolve to a canonical Git worktree root. `start(...)` SHALL NOT require a change identifier; no OpenSpec change needs to exist in the target repository at this point. If the worktree-root check fails, the orchestrator SHALL NOT create a workflow and SHALL NOT invoke any agent stage.

#### Scenario: workflow created
- **WHEN** the user starts a workflow with a task and target repository
- **THEN** a workflow is created, persisted, and positioned at the first stage

#### Scenario: workflow created when the target repository is a canonical Git worktree
- **WHEN** the target repository path resolves via `git rev-parse` to a valid worktree root
- **THEN** the orchestrator creates the workflow, persists its initial state, and the workflow proceeds toward the first agent stage

#### Scenario: workflow not created when the target repository is not a Git worktree
- **WHEN** the target repository path does not exist, or exists but does not resolve to a Git worktree
- **THEN** the orchestrator does not create a workflow and does not invoke any agent stage

### Requirement: Advance stages synchronously
The engine SHALL advance the workflow through stages in order, delegating agent stages to a stage runner and stopping at gates.

#### Scenario: agent stage
- **WHEN** the workflow advances to an agent stage
- **THEN** the engine delegates the stage to the stage runner and records the outcome

#### Scenario: gate reached
- **WHEN** the workflow reaches an approval or acceptance gate
- **THEN** the engine pauses and reports the workflow as awaiting user decision

### Requirement: Change-proposal stage binds the workflow to an OpenSpec change
The `CHANGE_PROPOSAL` stage's agent invocation SHALL produce a structured result naming the OpenSpec change identifier it created or revised. On a workflow's first successful `CHANGE_PROPOSAL` resolution, the orchestrator SHALL bind the workflow to that identifier, permanently for the workflow's lifetime, and SHALL run deterministic strict validation of that change's artifacts before the `VALIDATION_APPROVAL` gate is presented. A revision run SHALL confirm the same bound identifier; a resolution failure (missing, ambiguous, path-escaping identifier, or a validation failure) SHALL be recorded as a failed `CHANGE_PROPOSAL` outcome and SHALL return the workflow to `CHANGE_PROPOSAL` rather than presenting the gate.

#### Scenario: first successful proposal binds the workflow and validates before the gate opens
- **WHEN** `CHANGE_PROPOSAL` runs for the first time and its structured result resolves to exactly one active change whose artifacts pass strict validation
- **THEN** the orchestrator binds the workflow to that change and presents the `VALIDATION_APPROVAL` gate

#### Scenario: unresolved or invalid proposal returns to change-proposal without presenting the gate
- **WHEN** `CHANGE_PROPOSAL`'s structured result is missing, ambiguous, path-escaping, or resolves to a change whose artifacts fail strict validation
- **THEN** the orchestrator records a failed `CHANGE_PROPOSAL` outcome, does not present the `VALIDATION_APPROVAL` gate, and the workflow remains at or returns to `CHANGE_PROPOSAL`

#### Scenario: revision stays bound to the same change
- **WHEN** a bound workflow re-runs `CHANGE_PROPOSAL` for a revision and the structured result names the same identifier already bound
- **THEN** the orchestrator keeps the existing binding and re-validates that same change before re-presenting the gate

### Requirement: Approval gate
Before implementation, the user SHALL approve, reject, or request revisions of the proposal. Every gate action SHALL require the workflow to be at the `VALIDATION_APPROVAL` stage AND in `AWAITING_USER` status; if either precondition fails, the orchestrator SHALL reject the action and leave the workflow's stage and status unchanged. When the gate is presented, the orchestrator SHALL compute and record a fingerprint over the bound change's fingerprinted artifact files. Before recording an approval, the orchestrator SHALL recompute and compare that fingerprint; a mismatch SHALL block the approval from being recorded and SHALL reopen the gate with a freshly captured fingerprint instead. Before the implementation stage subsequently runs, the orchestrator SHALL verify the fingerprint again against the bound change's current artifact files and SHALL NOT allow implementation to proceed if drift is detected. Toggling a task checkbox in `tasks.md` alone SHALL NOT count as drift at either check.

#### Scenario: approved
- **WHEN** the user approves the proposal while the workflow is at `VALIDATION_APPROVAL` and `AWAITING_USER`, and the fingerprint recomputed at approval time matches the gate fingerprint
- **THEN** the workflow proceeds to implementation and the approval is recorded

#### Scenario: rejected
- **WHEN** the user rejects the proposal while the workflow is at `VALIDATION_APPROVAL` and `AWAITING_USER`
- **THEN** the workflow is cancelled and its stage no longer reads as an active gate for further approval or rejection

#### Scenario: revisions requested
- **WHEN** the user requests revisions while the workflow is at `VALIDATION_APPROVAL` and `AWAITING_USER`
- **THEN** the workflow returns to the change-proposal stage, remaining bound to the same change

#### Scenario: approve rejected while workflow is in progress
- **WHEN** the user calls approve while the workflow is at `VALIDATION_APPROVAL` but its status is `IN_PROGRESS` (mid-advance)
- **THEN** the orchestrator rejects the action and the workflow's stage and status are unchanged

#### Scenario: reject cannot resurrect a cancelled workflow
- **WHEN** the user rejects a proposal, and the user then calls approve again on the same workflow id
- **THEN** the second approve call is rejected because the workflow's status is `CANCELLED`, not `AWAITING_USER`, and the workflow remains cancelled

#### Scenario: approval blocked by fingerprint mismatch
- **WHEN** the user approves the proposal but the bound change's artifact files changed since the gate fingerprint was captured
- **THEN** the orchestrator does not record the approval, re-validates the current artifacts, and reopens the `VALIDATION_APPROVAL` gate with a freshly captured fingerprint

#### Scenario: implementation blocked by fingerprint mismatch after approval
- **WHEN** the bound change's artifact files are modified after approval and before the implementation stage runs
- **THEN** the orchestrator does not run the implementation stage and reopens the `VALIDATION_APPROVAL` gate with a freshly captured fingerprint

#### Scenario: a tasks.md checkbox toggle does not block approval or implementation
- **WHEN** the only change to the bound change's files since the last recorded fingerprint is a checkbox toggle in `tasks.md`
- **THEN** the fingerprint comparison at approval time and at implementation time both report no drift

### Requirement: Revision requests accept optional feedback
When the user requests revisions at the `VALIDATION_APPROVAL` gate, the orchestrator SHALL accept an optional free-text feedback message and make it available to the next `CHANGE_PROPOSAL` stage invocation.

#### Scenario: revisions requested with feedback
- **WHEN** the user requests revisions with feedback text while the workflow is at `VALIDATION_APPROVAL` and `AWAITING_USER`
- **THEN** the workflow returns to the `CHANGE_PROPOSAL` stage and the next `CHANGE_PROPOSAL` stage context includes that feedback

#### Scenario: revisions requested without feedback
- **WHEN** the user requests revisions with an empty feedback message
- **THEN** the request succeeds and the workflow returns to the `CHANGE_PROPOSAL` stage as if no specific feedback was supplied

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

### Requirement: Passing verification advances to review
A `PASSED` verification result SHALL advance the workflow to the `REVIEW` stage.

#### Scenario: passed verification proceeds to review
- **WHEN** the verification result is `PASSED`
- **THEN** the workflow moves to the `REVIEW` stage

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

### Requirement: Final acceptance
After a `REVIEW` outcome carrying a `ReviewVerdict` of `APPROVED`, the user SHALL accept the result or request further work, optionally supplying free-text feedback with a further-work request. Every acceptance action SHALL require the workflow to be at the `ACCEPTANCE` stage AND in `AWAITING_USER` status; if either precondition fails, the orchestrator SHALL reject the action and leave the workflow's stage and status unchanged.

#### Scenario: accepted
- **WHEN** the user accepts the result while the workflow is at `ACCEPTANCE` and `AWAITING_USER`
- **THEN** the workflow is marked completed

#### Scenario: further work requested
- **WHEN** the user requests further work while the workflow is at `ACCEPTANCE` and `AWAITING_USER`
- **THEN** the workflow returns to implementation

#### Scenario: further work requested with feedback
- **WHEN** the user requests further work with feedback text while the workflow is at `ACCEPTANCE` and `AWAITING_USER`
- **THEN** the workflow returns to implementation and the next `IMPLEMENTATION` stage context includes that feedback

#### Scenario: accept rejected against a non-awaiting workflow
- **WHEN** the user calls approve (accept) while the workflow is at `ACCEPTANCE` but its status is not `AWAITING_USER`
- **THEN** the orchestrator rejects the action and the workflow's stage and status are unchanged

### Requirement: Terminal states are immutable
A workflow whose status is `COMPLETED`, `CANCELLED`, or `FAILED` SHALL NOT be moved to any other stage or status by any gate command (`approve`, `reject`, `requestRevisions`, `requestFurtherWork`) or by `advance()`. No transition out of a terminal status exists in this capability; a future change may introduce an explicit, user-initiated retry or resume action.

#### Scenario: approve rejected on a completed workflow
- **WHEN** the user calls approve on a workflow whose status is `COMPLETED`
- **THEN** the orchestrator rejects the action and the workflow remains `COMPLETED` with its stage unchanged

#### Scenario: reject rejected on a cancelled workflow
- **WHEN** the user calls reject on a workflow whose status is already `CANCELLED`
- **THEN** the orchestrator rejects the action and the workflow remains `CANCELLED`

#### Scenario: requestFurtherWork rejected on a failed workflow
- **WHEN** the user calls requestFurtherWork on a workflow whose status is `FAILED`
- **THEN** the orchestrator rejects the action and the workflow remains `FAILED` with its stage unchanged

### Requirement: Persist workflow state
The orchestrator SHALL persist workflow state through a repository port and be able to load it back.

#### Scenario: save and load
- **WHEN** a workflow changes
- **THEN** its state is persisted, and loading it later returns the same state
