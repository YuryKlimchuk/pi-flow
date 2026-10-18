# Spec Delta

## Purpose

Defines the canonical-worktree-root repository preflight that SHALL run before any agent call, how a workflow binds to the OpenSpec change identifier produced by the `CHANGE_PROPOSAL` stage's structured result, the deterministic strict validation that SHALL run before the approval gate is presented, and the artifact fingerprint that detects drift between gate-open, approval, and implementation while exempting `tasks.md` checkbox-only edits.

## ADDED Requirements

### Requirement: Target repository preflight resolves a canonical Git worktree root before any agent call
Before any agent stage invokes Pi for a workflow - including the first stage that does so - the orchestrator SHALL resolve the target repository path to a canonical Git worktree root using `git rev-parse` (e.g. `--show-toplevel` and/or `--git-dir`), not a bare filesystem check of a `.git` entry's shape. A path that does not exist, or that exists but does not resolve to a Git worktree, SHALL be rejected before any agent call is made. A Git worktree with no commits yet (freshly initialized, empty history) SHALL be accepted. A linked worktree (not just the main worktree) SHALL be accepted.

#### Scenario: canonical worktree root is accepted
- **WHEN** the target repository path resolves via `git rev-parse` to a valid worktree root
- **THEN** the orchestrator accepts the path and proceeds to run the first agent stage

#### Scenario: freshly initialized empty repository is accepted
- **WHEN** the target repository path is a Git worktree with no commits yet
- **THEN** the orchestrator accepts the path and does not reject the workflow on that basis alone

#### Scenario: linked worktree is accepted
- **WHEN** the target repository path is a linked Git worktree (not the repository's main worktree)
- **THEN** the orchestrator accepts the path via `git rev-parse` resolution and proceeds

#### Scenario: non-existent path is rejected before any agent call
- **WHEN** the target repository path does not exist on disk
- **THEN** the orchestrator rejects the request before any agent stage runs, and no workflow is created

#### Scenario: non-worktree directory is rejected before any agent call
- **WHEN** the target repository path is an existing directory that `git rev-parse` reports is not inside a Git worktree
- **THEN** the orchestrator rejects the request before any agent stage runs, and no workflow is created

#### Scenario: preflight precedes every agent invocation
- **WHEN** a workflow is started
- **THEN** the canonical-worktree-root preflight completes successfully before `PROJECT_ANALYSIS` or any other agent stage invokes Pi for that workflow

### Requirement: Starting a workflow requires no change identifier
`WorkflowEngine.start(...)` SHALL accept only the task description and target repository path; it SHALL NOT require a change identifier as an argument, and SHALL NOT fail a start request for lacking one. No OpenSpec change is expected to exist in the target repository at `start(...)` time.

#### Scenario: workflow starts against a repository with no OpenSpec changes yet
- **WHEN** a start request is made against a target repository that currently has no entries under `openspec/changes/`
- **THEN** the orchestrator does not reject the request for that reason, and the workflow proceeds through `TASK_SETUP` and `PROJECT_ANALYSIS` with no change bound yet

#### Scenario: start request accepts exactly the existing argument shape
- **WHEN** a start request supplies a task description and a target repository path and nothing else
- **THEN** the orchestrator processes the request without requiring any additional argument

### Requirement: Change-proposal stage produces a structured change identifier
The `CHANGE_PROPOSAL` stage's agent invocation SHALL require a structured result that names the exact OpenSpec change identifier (change directory name) the architect created or revised. A `CHANGE_PROPOSAL` run whose structured result omits the identifier, or whose result cannot be parsed as the expected structured shape, SHALL be treated as a failed `CHANGE_PROPOSAL` outcome.

#### Scenario: structured result names the created change
- **WHEN** the `CHANGE_PROPOSAL` stage's agent run completes successfully
- **THEN** its structured result names the exact change directory identifier the architect created or revised

#### Scenario: missing identifier in an otherwise-successful run is a stage failure
- **WHEN** the `CHANGE_PROPOSAL` stage's underlying agent run reports technical success but its structured result does not name a change identifier
- **THEN** the orchestrator records the `CHANGE_PROPOSAL` outcome as failed and does not proceed to bind, validate, or present the approval gate

### Requirement: Change identifier is resolved via the OpenSpec CLI and rejected if missing, ambiguous, or path-escaping
Immediately after a successful `CHANGE_PROPOSAL` run, the orchestrator SHALL resolve the reported change identifier against the target repository's actual OpenSpec changes using the OpenSpec CLI, not a raw filesystem scan. Resolution SHALL fail - and the `CHANGE_PROPOSAL` outcome SHALL be recorded as failed - if the identifier matches zero active changes, matches more than one active change, or would resolve to a location outside the target repository's OpenSpec change store (including identifiers containing path-traversal segments or absolute paths pointing elsewhere). No later-stage agent call SHALL be made for a workflow whose `CHANGE_PROPOSAL` resolution fails this check.

#### Scenario: identifier resolves to exactly one active change
- **WHEN** the reported change identifier matches exactly one active change known to the OpenSpec CLI in the target repository
- **THEN** resolution succeeds and the workflow proceeds to bind to that change

#### Scenario: identifier matching no active change is rejected
- **WHEN** the reported change identifier matches no active change known to the OpenSpec CLI
- **THEN** the orchestrator records the `CHANGE_PROPOSAL` outcome as failed, does not bind the workflow to any change, and makes no later-stage agent call

#### Scenario: identifier matching more than one active change is rejected
- **WHEN** the reported change identifier matches more than one active change
- **THEN** the orchestrator records the `CHANGE_PROPOSAL` outcome as failed, does not bind the workflow to any change, and makes no later-stage agent call

#### Scenario: path-escaping identifier is rejected
- **WHEN** the reported change identifier contains a path-traversal segment or otherwise resolves outside the target repository's OpenSpec change store
- **THEN** the orchestrator records the `CHANGE_PROPOSAL` outcome as failed, does not bind the workflow to any change, and makes no later-stage agent call

### Requirement: Workflow binds permanently to the first successfully resolved change identifier
On the first successful resolution of a change identifier for a workflow, the orchestrator SHALL record that identifier as the workflow's bound change and SHALL treat it as immutable for the remainder of the workflow's lifetime. Every later stage that needs a change identifier (including `IMPLEMENTATION` and `REVIEW`) SHALL use the bound identifier and SHALL NOT re-resolve or substitute a different one.

#### Scenario: binding is recorded on first success
- **WHEN** a workflow's `CHANGE_PROPOSAL` run resolves successfully for the first time
- **THEN** the orchestrator records the resolved identifier as the workflow's bound change

#### Scenario: later stages use the bound identifier
- **WHEN** `IMPLEMENTATION` or `REVIEW` runs for a workflow that has a bound change
- **THEN** the stage's context references the bound change identifier recorded at binding time

### Requirement: Revisions stay bound to the same change
When a workflow returns to `CHANGE_PROPOSAL` after binding - whether via a user-requested revision or via a validation failure - the revision run SHALL revise the already-bound change. If the structured result of a revision run names a change identifier different from the one already bound, the orchestrator SHALL treat this as a failed `CHANGE_PROPOSAL` outcome rather than rebinding to the new identifier.

#### Scenario: revision run keeps the existing binding
- **WHEN** a bound workflow returns to `CHANGE_PROPOSAL` for a revision and the revision run's structured result names the same identifier already bound
- **THEN** the orchestrator keeps the existing binding and proceeds to re-validate that same change

#### Scenario: revision run naming a different identifier is rejected
- **WHEN** a bound workflow's revision run reports a structured result naming a change identifier different from the one already bound
- **THEN** the orchestrator records the `CHANGE_PROPOSAL` outcome as failed and does not rebind the workflow to the newly named identifier

### Requirement: Deterministic strict validation runs before the approval gate is presented
After a workflow binds (or re-binds-by-confirmation on a revision) to a change, and before the `VALIDATION_APPROVAL` gate is presented to the user, the orchestrator SHALL run deterministic, local-only strict validation of the bound change's artifacts (equivalent to `openspec validate <changeId> --strict`) as a bounded local process. If validation fails, the orchestrator SHALL NOT present the approval gate, SHALL record the validation failure as the `CHANGE_PROPOSAL` outcome's reason, and SHALL route the workflow back to `CHANGE_PROPOSAL` instead.

#### Scenario: valid artifacts allow the gate to open
- **WHEN** the bound change's artifacts pass strict validation
- **THEN** the orchestrator presents the `VALIDATION_APPROVAL` gate to the user

#### Scenario: invalid artifacts block the gate from opening
- **WHEN** the bound change's artifacts fail strict validation
- **THEN** the orchestrator does not present the `VALIDATION_APPROVAL` gate, records the validation failure as the reason, and routes the workflow back to `CHANGE_PROPOSAL`

#### Scenario: validation precedes gate presentation on every binding or re-binding
- **WHEN** a workflow binds to a change for the first time, or confirms the same binding on a revision
- **THEN** strict validation of that change completes and passes before the `VALIDATION_APPROVAL` gate is presented

### Requirement: Artifact fingerprint is captured at gate-open and rechecked before approval is recorded and before implementation
When the `VALIDATION_APPROVAL` gate is presented to the user, the orchestrator SHALL compute a deterministic digest over the bound change's fingerprinted file set (see the file-set requirement below) and record it as the gate fingerprint. Before recording an approval, the orchestrator SHALL recompute the digest and compare it to the recorded gate fingerprint; a mismatch SHALL prevent the approval from being recorded, SHALL trigger re-validation of the current artifacts, and SHALL re-present the gate with a freshly captured fingerprint instead of recording approval against stale content. The same recompute-and-compare check SHALL run again immediately before the implementation stage runs; a mismatch detected there SHALL block implementation, trigger re-validation, and re-present the gate the same way.

#### Scenario: fingerprint is captured when the gate opens
- **WHEN** the `VALIDATION_APPROVAL` gate is presented to the user
- **THEN** the orchestrator computes a digest over the bound change's fingerprinted file set and records it as the gate fingerprint

#### Scenario: unchanged artifacts allow approval to be recorded
- **WHEN** the user approves the proposal and the bound change's fingerprinted files are unchanged since the gate fingerprint was captured
- **THEN** the orchestrator records the approval and proceeds toward implementation

#### Scenario: drift detected at approval time blocks the approval and reopens the gate
- **WHEN** the user approves the proposal but the bound change's fingerprinted files have changed since the gate fingerprint was captured
- **THEN** the orchestrator does not record the approval, re-validates the current artifacts, and re-presents the `VALIDATION_APPROVAL` gate with a freshly captured fingerprint

#### Scenario: unchanged artifacts allow implementation to proceed
- **WHEN** the implementation stage is about to run and the bound change's fingerprinted files are unchanged since the most recently recorded fingerprint
- **THEN** the orchestrator proceeds to run the implementation stage

#### Scenario: drift detected before implementation blocks it and reopens the gate
- **WHEN** the implementation stage is about to run and the bound change's fingerprinted files have changed since the most recently recorded fingerprint
- **THEN** the orchestrator does not run the implementation stage, re-validates the current artifacts, and re-presents the `VALIDATION_APPROVAL` gate with a freshly captured fingerprint

### Requirement: Fingerprinted file set is defined deterministically and exempts tasks.md checkbox state
The fingerprint SHALL cover a deterministic set of the bound change's files: `proposal.md`, `design.md`, every file under `specs/` (sorted by relative path), and `tasks.md` with its checkbox markers (`- [ ]` and `- [x]`) normalized to a single fixed form before digesting. A change to `tasks.md` consisting only of toggling one or more checkbox markers SHALL NOT change the computed digest and SHALL NOT be treated as drift. Any other change to `tasks.md`'s content, or any change to `proposal.md`, `design.md`, or any file under `specs/`, SHALL change the computed digest and SHALL be treated as drift.

#### Scenario: checking a task checkbox does not trigger drift
- **WHEN** the only change to the bound change's files since the last recorded fingerprint is a task line in `tasks.md` changing from `- [ ]` to `- [x]` (or vice versa)
- **THEN** the recomputed digest matches the recorded fingerprint and no drift is reported

#### Scenario: a non-checkbox edit to tasks.md triggers drift
- **WHEN** a task's descriptive text in `tasks.md` is edited, independent of any checkbox state change
- **THEN** the recomputed digest differs from the recorded fingerprint and drift is reported

#### Scenario: an edit to proposal.md, design.md, or a spec file triggers drift
- **WHEN** any content in `proposal.md`, `design.md`, or a file under `specs/` changes
- **THEN** the recomputed digest differs from the recorded fingerprint and drift is reported

### Requirement: Binding, preflight, resolution, validation, and fingerprinting make no Git write operations
The orchestrator SHALL NOT perform any Git write operation (commit, reset, stash, checkout, or clean) against the target repository as part of the worktree-root preflight, change-identifier resolution, strict validation, or fingerprint computation/verification. A target repository with uncommitted changes SHALL be accepted at every step and its working tree SHALL be left unmodified by these checks.

#### Scenario: dirty worktree is accepted without modification
- **WHEN** the target repository has uncommitted changes at any point during preflight, resolution, validation, or fingerprinting
- **THEN** the orchestrator performs all of those checks without committing, resetting, stashing, checking out, or cleaning any file in the target repository

#### Scenario: fingerprinting does not alter the working tree
- **WHEN** the orchestrator computes or verifies the artifact fingerprint
- **THEN** no file in the target repository is created, modified, or removed by that computation
