# workflow-orchestration Specification

## Purpose
Orchestrates the pi-flow development workflow as a synchronous state machine over stages, approval gates, and correction loops, with persisted workflow state.

## Requirements

### Requirement: Start workflow
Given a task description and a target repository path, the orchestrator SHALL create a workflow and persist its initial state.

#### Scenario: workflow created
- **WHEN** the user starts a workflow with a task and target repository
- **THEN** a workflow is created, persisted, and positioned at the first stage

### Requirement: Advance stages synchronously
The engine SHALL advance the workflow through stages in order, delegating agent stages to a stage runner and stopping at gates.

#### Scenario: agent stage
- **WHEN** the workflow advances to an agent stage
- **THEN** the engine delegates the stage to the stage runner and records the outcome

#### Scenario: gate reached
- **WHEN** the workflow reaches an approval or acceptance gate
- **THEN** the engine pauses and reports the workflow as awaiting user decision

### Requirement: Approval gate
Before implementation, the user SHALL approve, reject, or request revisions of the proposal. Every gate action SHALL require the workflow to be at the `VALIDATION_APPROVAL` stage AND in `AWAITING_USER` status; if either precondition fails, the orchestrator SHALL reject the action and leave the workflow's stage and status unchanged.

#### Scenario: approved
- **WHEN** the user approves the proposal while the workflow is at `VALIDATION_APPROVAL` and `AWAITING_USER`
- **THEN** the workflow proceeds to implementation

#### Scenario: rejected
- **WHEN** the user rejects the proposal while the workflow is at `VALIDATION_APPROVAL` and `AWAITING_USER`
- **THEN** the workflow is cancelled and its stage no longer reads as an active gate for further approval or rejection

#### Scenario: revisions requested
- **WHEN** the user requests revisions while the workflow is at `VALIDATION_APPROVAL` and `AWAITING_USER`
- **THEN** the workflow returns to the change-proposal stage

#### Scenario: approve rejected while workflow is in progress
- **WHEN** the user calls approve while the workflow is at `VALIDATION_APPROVAL` but its status is `IN_PROGRESS` (mid-advance)
- **THEN** the orchestrator rejects the action and the workflow's stage and status are unchanged

#### Scenario: reject cannot resurrect a cancelled workflow
- **WHEN** the user rejects a proposal, and the user then calls approve again on the same workflow id
- **THEN** the second approve call is rejected because the workflow's status is `CANCELLED`, not `AWAITING_USER`, and the workflow remains cancelled

### Requirement: Revision requests accept optional feedback
When the user requests revisions at the `VALIDATION_APPROVAL` gate, the orchestrator SHALL accept an optional free-text feedback message and make it available to the next `CHANGE_PROPOSAL` stage invocation.

#### Scenario: revisions requested with feedback
- **WHEN** the user requests revisions with feedback text while the workflow is at `VALIDATION_APPROVAL` and `AWAITING_USER`
- **THEN** the workflow returns to the `CHANGE_PROPOSAL` stage and the next `CHANGE_PROPOSAL` stage context includes that feedback

#### Scenario: revisions requested without feedback
- **WHEN** the user requests revisions with an empty feedback message
- **THEN** the request succeeds and the workflow returns to the `CHANGE_PROPOSAL` stage as if no specific feedback was supplied

### Requirement: Correction loop
A failed verification, a `REVIEW` outcome carrying a `ReviewVerdict` of `CHANGES_REQUIRED`, a technical failure of the `REVIEW` stage's execution, or a failed verdict extraction SHALL return the workflow to implementation, except that a technical failure or a failed extraction SHALL mark the workflow `FAILED` instead of silently looping.

#### Scenario: verification failed
- **WHEN** automated verification fails
- **THEN** the workflow returns to implementation

#### Scenario: review requires corrections
- **WHEN** the `REVIEW` stage outcome carries a `ReviewVerdict` of `CHANGES_REQUIRED`
- **THEN** the workflow returns to implementation

#### Scenario: stage execution raises an exception
- **WHEN** the stage runner throws an exception while executing an agent stage
- **THEN** the orchestrator records a failed stage outcome describing the exception and marks the workflow `FAILED`, instead of propagating the exception

#### Scenario: verification raises an exception
- **WHEN** the verifier throws an exception during the verification stage
- **THEN** the orchestrator records a failed stage outcome describing the exception and marks the workflow `FAILED`, instead of propagating the exception

#### Scenario: fatal JVM errors are not caught
- **WHEN** the stage runner or verifier throws an `Error` that represents a fatal JVM condition (for example `OutOfMemoryError` or `StackOverflowError`)
- **THEN** the orchestrator does not catch it as a failed stage outcome and lets it propagate

#### Scenario: review technical failure marks the workflow failed instead of looping
- **WHEN** the `REVIEW` stage's execution fails for a technical reason (process crash, timeout, or a failed agent run)
- **THEN** the orchestrator marks the workflow `FAILED` instead of returning it to `IMPLEMENTATION`

#### Scenario: failed verdict extraction marks the workflow failed instead of looping
- **WHEN** the `REVIEW` stage executes without a technical error but extraction of its output into a `ReviewVerdict` and findings fails
- **THEN** the orchestrator marks the workflow `FAILED` instead of returning it to `IMPLEMENTATION` or advancing to `ACCEPTANCE`

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
