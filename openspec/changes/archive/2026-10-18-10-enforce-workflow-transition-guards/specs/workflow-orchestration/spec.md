# Spec Delta

## MODIFIED Requirements

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

### Requirement: Correction loop
A failed verification or a review requiring corrections SHALL return the workflow to implementation.

#### Scenario: verification failed
- **WHEN** automated verification fails
- **THEN** the workflow returns to implementation

#### Scenario: review requires corrections
- **WHEN** the review requires corrections
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

### Requirement: Final acceptance
After a passing review, the user SHALL accept the result or request further work. Every acceptance action SHALL require the workflow to be at the `ACCEPTANCE` stage AND in `AWAITING_USER` status; if either precondition fails, the orchestrator SHALL reject the action and leave the workflow's stage and status unchanged.

#### Scenario: accepted
- **WHEN** the user accepts the result while the workflow is at `ACCEPTANCE` and `AWAITING_USER`
- **THEN** the workflow is marked completed

#### Scenario: further work requested
- **WHEN** the user requests further work while the workflow is at `ACCEPTANCE` and `AWAITING_USER`
- **THEN** the workflow returns to implementation

#### Scenario: accept rejected against a non-awaiting workflow
- **WHEN** the user calls approve (accept) while the workflow is at `ACCEPTANCE` but its status is not `AWAITING_USER`
- **THEN** the orchestrator rejects the action and the workflow's stage and status are unchanged

## ADDED Requirements

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
