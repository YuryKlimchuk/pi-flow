# Spec Delta

## Purpose

Defines the typed context the orchestrator carries into each agent stage invocation, and how user-supplied revision/further-work feedback is captured and threaded forward to the next relevant stage run.

## ADDED Requirements

### Requirement: Typed stage context carries task, repo, and prior findings
The orchestrator SHALL build a typed stage context for every agent stage invocation containing the workflow's task description, target repository path, the `PROJECT_ANALYSIS` stage's recorded findings (once that stage has run), and the `CHANGE_PROPOSAL` stage's recorded proposal output (once that stage has run). Each agent stage invocation SHALL receive this typed context instead of only the task description and target repository path.

#### Scenario: implementation stage receives prior analysis and proposal
- **WHEN** the workflow reaches `IMPLEMENTATION` after `PROJECT_ANALYSIS` and `CHANGE_PROPOSAL` have both recorded successful outcomes
- **THEN** the stage context passed into the `IMPLEMENTATION` invocation includes the recorded `PROJECT_ANALYSIS` findings and the recorded `CHANGE_PROPOSAL` output

#### Scenario: review stage receives prior analysis and proposal
- **WHEN** the workflow reaches `REVIEW`
- **THEN** the stage context passed into the `REVIEW` invocation includes the recorded `PROJECT_ANALYSIS` findings and the recorded `CHANGE_PROPOSAL` output

#### Scenario: first stage has no prior findings
- **WHEN** the workflow invokes the `PROJECT_ANALYSIS` stage for the first time
- **THEN** the stage context passed into that invocation has no `PROJECT_ANALYSIS` findings and no `CHANGE_PROPOSAL` output, since neither has run yet

### Requirement: User revision and further-work feedback is captured
The orchestrator SHALL accept an optional free-text feedback message when the user requests revisions at the `VALIDATION_APPROVAL` gate or requests further work at the `ACCEPTANCE` gate, and SHALL persist that feedback as part of the workflow's state. An empty or absent feedback message SHALL be accepted and treated as "no specific feedback supplied," not as an error.

#### Scenario: revision feedback is accepted and persisted
- **WHEN** the user requests revisions at `VALIDATION_APPROVAL` with the feedback text "the error handling section is missing"
- **THEN** the workflow's persisted state records that feedback

#### Scenario: further-work feedback is accepted and persisted
- **WHEN** the user requests further work at `ACCEPTANCE` with the feedback text "please add a test for the empty-input case"
- **THEN** the workflow's persisted state records that feedback

#### Scenario: empty feedback is not an error
- **WHEN** the user requests revisions or further work with an empty feedback string
- **THEN** the request succeeds and the workflow proceeds as if no specific feedback was supplied

### Requirement: Pending feedback is threaded into the next relevant stage context
Once user feedback has been recorded via a revision or further-work request, the orchestrator SHALL include that feedback in the typed stage context passed to the next invocation of the stage the workflow returns to (`CHANGE_PROPOSAL` after a revision request, `IMPLEMENTATION` after a further-work request). After that stage has consumed the feedback by running once, the orchestrator SHALL clear the pending feedback so it is not re-applied to later, unrelated stage runs.

#### Scenario: revision feedback reaches the re-run proposal stage
- **WHEN** the user requests revisions with feedback "the error handling section is missing," and the workflow re-runs `CHANGE_PROPOSAL`
- **THEN** the stage context for that `CHANGE_PROPOSAL` invocation includes the feedback text "the error handling section is missing"

#### Scenario: further-work feedback reaches the re-run implementation stage
- **WHEN** the user requests further work with feedback "please add a test for the empty-input case," and the workflow re-runs `IMPLEMENTATION`
- **THEN** the stage context for that `IMPLEMENTATION` invocation includes the feedback text "please add a test for the empty-input case"

#### Scenario: consumed feedback does not leak into later stages
- **WHEN** the user requests revisions with feedback, the workflow re-runs `CHANGE_PROPOSAL` once, and the workflow later proceeds to `IMPLEMENTATION` without a new feedback request
- **THEN** the stage context for that `IMPLEMENTATION` invocation does not include the earlier revision feedback
