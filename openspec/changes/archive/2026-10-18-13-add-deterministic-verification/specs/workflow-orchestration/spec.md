# Spec Delta

## MODIFIED Requirements

### Requirement: Correction loop
A `FAILED` verification result, a `REVIEW` outcome carrying a `ReviewVerdict` of `CHANGES_REQUIRED`, a technical failure of the `REVIEW` stage's execution, or a failed review verdict extraction SHALL return the workflow to implementation, except that a technical failure or a failed extraction SHALL mark the workflow `FAILED` instead of silently looping. A `NOT_CONFIGURED` or `INFRASTRUCTURE_ERROR` verification result SHALL NOT be treated as a correction and SHALL NOT return the workflow to implementation.

#### Scenario: verification failed
- **WHEN** the verification result is `FAILED`
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

#### Scenario: unconfigured verification does not trigger a correction loop
- **WHEN** the verification result is `NOT_CONFIGURED`
- **THEN** the orchestrator does not return the workflow to `IMPLEMENTATION` as if a correction were requested

#### Scenario: a verification infrastructure error does not trigger a correction loop
- **WHEN** the verification result is `INFRASTRUCTURE_ERROR`
- **THEN** the orchestrator does not return the workflow to `IMPLEMENTATION` as if a correction were requested, and does not treat the result as equivalent to `FAILED`

## ADDED Requirements

### Requirement: Passing verification advances to review
A `PASSED` verification result SHALL advance the workflow to the `REVIEW` stage.

#### Scenario: passed verification proceeds to review
- **WHEN** the verification result is `PASSED`
- **THEN** the workflow moves to the `REVIEW` stage

### Requirement: Unconfigured or broken verification configuration blocks automatic progression with an explicit, recoverable state
When the verification result is `NOT_CONFIGURED` or `INFRASTRUCTURE_ERROR`, the orchestrator SHALL NOT advance the workflow to `REVIEW` and SHALL NOT return it to `IMPLEMENTATION` as a correction. The orchestrator SHALL instead leave the workflow in a state that explicitly names the specific cause (missing configuration file vs. a specific infrastructure problem) and that is NOT a terminal status (`COMPLETED`, `CANCELLED`, `FAILED`); the workflow SHALL remain recoverable, through a retry action available while the workflow is in this state, without requiring the application to be restarted.

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
