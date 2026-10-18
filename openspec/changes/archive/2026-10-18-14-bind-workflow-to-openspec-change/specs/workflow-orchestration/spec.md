# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

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
