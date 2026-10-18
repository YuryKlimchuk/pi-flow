# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: Revision requests accept optional feedback
When the user requests revisions at the `VALIDATION_APPROVAL` gate, the orchestrator SHALL accept an optional free-text feedback message and make it available to the next `CHANGE_PROPOSAL` stage invocation.

#### Scenario: revisions requested with feedback
- **WHEN** the user requests revisions with feedback text while the workflow is at `VALIDATION_APPROVAL` and `AWAITING_USER`
- **THEN** the workflow returns to the `CHANGE_PROPOSAL` stage and the next `CHANGE_PROPOSAL` stage context includes that feedback

#### Scenario: revisions requested without feedback
- **WHEN** the user requests revisions with an empty feedback message
- **THEN** the request succeeds and the workflow returns to the `CHANGE_PROPOSAL` stage as if no specific feedback was supplied
