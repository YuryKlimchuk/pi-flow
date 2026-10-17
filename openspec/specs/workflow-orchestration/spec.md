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
Before implementation, the user SHALL approve, reject, or request revisions of the proposal.

#### Scenario: approved
- **WHEN** the user approves the proposal
- **THEN** the workflow proceeds to implementation

#### Scenario: rejected
- **WHEN** the user rejects the proposal
- **THEN** the workflow is cancelled

#### Scenario: revisions requested
- **WHEN** the user requests revisions
- **THEN** the workflow returns to the change-proposal stage

### Requirement: Correction loop
A failed verification or a review requiring corrections SHALL return the workflow to implementation.

#### Scenario: verification failed
- **WHEN** automated verification fails
- **THEN** the workflow returns to implementation

#### Scenario: review requires corrections
- **WHEN** the review requires corrections
- **THEN** the workflow returns to implementation

### Requirement: Final acceptance
After a passing review, the user SHALL accept the result or request further work.

#### Scenario: accepted
- **WHEN** the user accepts the result
- **THEN** the workflow is marked completed

#### Scenario: further work requested
- **WHEN** the user requests further work
- **THEN** the workflow returns to implementation

### Requirement: Persist workflow state
The orchestrator SHALL persist workflow state through a repository port and be able to load it back.

#### Scenario: save and load
- **WHEN** a workflow changes
- **THEN** its state is persisted, and loading it later returns the same state
