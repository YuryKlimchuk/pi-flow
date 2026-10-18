# Spec Delta

## Purpose

Gives a user an inspectable, in-memory record of each workflow stage attempt's timing, outcome category, and a safe (prompt/response/secret-free) reason, derived from plan 15's attempt history and surfaced through logs and a shell command, without duplicating or replacing that history.

## ADDED Requirements

### Requirement: Diagnostics is sequenced after, and reuses, plans 10-15
This capability SHALL be implemented only after plans 10, 11, 12, 13, 14, and 15 are implemented, and SHALL read its attempt identity (stage, attempt sequence number, attempt kind) from plan 15's `Workflow`/`StageOutcome` attempt-history fields. This capability SHALL NOT define or maintain an independent attempt counter or attempt-sequencing scheme, and SHALL NOT provide a speculative fallback mode for operating against a workflow engine where plans 10-15 are not implemented.

#### Scenario: attempt sequence number is read from plan 15's history, not recomputed
- **WHEN** a diagnostic entry is recorded for a stage attempt
- **THEN** the entry's attempt sequence number is the value plan 15's attempt-history mechanism already assigned to that attempt, not an independently incremented counter

#### Scenario: diagnostics capability is not implemented ahead of its predecessors
- **WHEN** plans 10 through 15 are not yet implemented in the codebase
- **THEN** this capability's recording and reporting behavior is not present and is not expected to operate

### Requirement: Record a diagnostic entry per stage attempt
The orchestrator SHALL record one diagnostic entry for every stage attempt a workflow makes (an initial attempt, a technical retry, or a content-correction loop iteration, per plan 15's `AttemptKind`), carrying the workflow id, the stage, the attempt sequence number (sourced from plan 15), a start timestamp, a duration, a category, and a reason string.

#### Scenario: initial attempt recorded
- **WHEN** a workflow's `PROJECT_ANALYSIS` stage runs for the first time and completes
- **THEN** a diagnostic entry is recorded for that attempt with stage `PROJECT_ANALYSIS`, attempt sequence number `1`, a non-negative duration, and a category reflecting the outcome

#### Scenario: retry attempt recorded separately
- **WHEN** plan 15's `retryStage` re-runs a stage after a technical failure
- **THEN** a second diagnostic entry is recorded for the same stage with the incremented attempt sequence number plan 15's history assigned to that retry, distinct from the first entry

#### Scenario: content-correction attempt recorded separately
- **WHEN** a review or verification outcome routes the workflow back to implementation for a content correction (plan 15's `CONTENT_CORRECTION` attempt kind)
- **THEN** a diagnostic entry is recorded for the resulting implementation attempt, distinguishable by category from a technical-retry attempt

### Requirement: Classify each attempt with a closed category
Each diagnostic entry SHALL carry exactly one category from a closed, machine-readable set that distinguishes a successful attempt from a technical failure, a content correction, a timeout, and a not-configured verification pause, mapped from plan 12's `ReviewVerdict`, plan 13's `VerificationResult`, and plan 15's `AttemptKind`.

#### Scenario: successful attempt categorized
- **WHEN** a stage attempt completes successfully
- **THEN** its diagnostic entry's category identifies it as successful

#### Scenario: technical failure categorized distinctly from content correction
- **WHEN** one attempt fails for a technical reason (process crash, timeout, malformed output) and a separate attempt fails because review or verification required another pass
- **THEN** the two attempts' diagnostic entries carry different categories

### Requirement: Exclude prompt, provider output, and secrets from recorded reasons
A diagnostic entry's reason string SHALL be built only from stage name, category, status/exit codes, and existing process/protocol-level diagnostic text; it SHALL NOT contain agent prompt text, agent response/final text, or any credential, API key, or other secret value.

#### Scenario: reason omits final text
- **WHEN** a stage attempt completes with a known, non-empty agent final text value
- **THEN** the diagnostic entry's reason string does not contain that final text value

#### Scenario: reason omits prompt text
- **WHEN** a stage attempt is built from a known, non-empty prompt value
- **THEN** the diagnostic entry's reason string does not contain that prompt value

#### Scenario: reason omits secret values
- **WHEN** a stage attempt's execution environment includes a configured credential or API key value
- **THEN** the diagnostic entry's reason string does not contain that credential or API key value

### Requirement: Log each diagnostic entry at INFO level without prompt, response, or secret content
The orchestrator SHALL log each recorded diagnostic entry at `INFO` level through the project's existing logging setup, in addition to retaining it in memory. This logging SHALL carry the same content restrictions as the recorded reason: no agent prompt text, no agent response/final text, and no secret value.

#### Scenario: entry logged on recording
- **WHEN** a diagnostic entry is recorded for a stage attempt
- **THEN** an `INFO`-level log line is emitted containing that entry's stage, attempt sequence number, category, and duration

#### Scenario: logged entry omits prompt, response, and secret content
- **WHEN** a diagnostic entry's `INFO`-level log line is emitted
- **THEN** that log line does not contain the stage attempt's prompt text, final/response text, or any configured secret value

### Requirement: JsonModePiRunner's existing log lines are corrected to remove prompt, argv, and final-text leakage
`JsonModePiRunner`'s `INFO`-level log line emitted when starting a Pi run SHALL NOT include the raw prompt text or the full child-process argument list verbatim. `JsonModePiRunner`'s `INFO`-level log line emitted when a Pi run finishes SHALL NOT include the agent's final/response text, truncated or not.

#### Scenario: run-start log omits prompt and full argv
- **WHEN** `JsonModePiRunner` starts a Pi child process and logs the run start
- **THEN** the emitted log line does not contain the prompt text passed to that run and does not contain the full argument list verbatim

#### Scenario: run-finish log omits final text
- **WHEN** `JsonModePiRunner` completes a Pi run and logs the run result
- **THEN** the emitted log line does not contain the run's final/response text in any form

### Requirement: Diagnostics is a bounded reporting projection, not a replacement for the full attempt history
`Workflow.outcomes()` (plan 15's full, unbounded, append-only attempt history) SHALL remain the system of record for a workflow's complete attempt sequence and SHALL NOT be truncated, reordered, or have any entry dropped by this capability. This capability's `WorkflowDiagnostics` record is a separate, bounded, truncatable projection derived from that history for display and logging, and SHALL NOT be used as the basis for retry eligibility, budget enforcement, or any other decision reserved to plan 15's workflow-orchestration logic.

#### Scenario: full history remains intact when diagnostics entries are dropped
- **WHEN** a workflow's diagnostics buffer exceeds its configured entry cap and the oldest diagnostic entries are dropped
- **THEN** `Workflow.outcomes()` still contains every attempt's `StageOutcome` record, with none dropped or altered

#### Scenario: diagnostics is not consulted for retry or budget decisions
- **WHEN** the orchestrator evaluates whether a stage is retry-eligible or whether a retry/correction budget is exhausted
- **THEN** that evaluation is based on plan 15's `Workflow`/`StageOutcome` attempt history, not on this capability's `WorkflowDiagnostics` record

### Requirement: Bound the in-memory diagnostics buffer per workflow
The number of diagnostic entries retained per workflow SHALL be capped at a configurable, defaulted limit; once the cap is exceeded, the oldest entries SHALL be dropped and the workflow's diagnostics record SHALL report that entries were dropped.

#### Scenario: entries within cap retained in full
- **WHEN** a workflow's recorded entry count is at or below the configured per-workflow cap
- **THEN** every recorded entry is retrievable and the diagnostics record reports no entries dropped

#### Scenario: cap exceeded drops oldest entries
- **WHEN** a workflow's recorded entry count exceeds the configured per-workflow cap
- **THEN** the oldest entries beyond the cap are no longer retrievable and the diagnostics record reports that entries were dropped

### Requirement: Bound each diagnostic entry's reason length
Each diagnostic entry's reason string SHALL be capped at a configurable, defaulted maximum length; a reason longer than the cap SHALL be truncated to the cap and the entry SHALL indicate that its reason was truncated.

#### Scenario: reason within cap unchanged
- **WHEN** a diagnostic entry's computed reason text is at or below the configured per-entry reason-length cap
- **THEN** the entry's reason is recorded unchanged and marked as not truncated

#### Scenario: reason exceeding cap truncated
- **WHEN** a diagnostic entry's computed reason text exceeds the configured per-entry reason-length cap
- **THEN** the entry's recorded reason is truncated to the cap and the entry is marked as truncated

### Requirement: Raw process output capture remains bounded and non-blocking
Any stdout/stderr content from a Pi run that contributes to a diagnostic entry's reason SHALL be drained through the orchestrator's existing non-blocking reader mechanism and SHALL be subject to the same reason-length cap as any other reason content. Building a diagnostic entry SHALL NOT introduce a blocking read of process output or an unbounded in-memory accumulation of raw process output.

#### Scenario: diagnostic recording does not block on process output
- **WHEN** a diagnostic entry is built from a stage attempt's process output
- **THEN** the recording uses the existing non-blocking stdout/stderr drain and does not synchronously block waiting for additional output beyond what that drain already captured

#### Scenario: process output contributing to a reason is capped
- **WHEN** a stage attempt's captured process output exceeds the configured reason-length cap
- **THEN** the portion included in the diagnostic entry's reason is truncated to the cap, consistent with the reason-length-cap requirement

### Requirement: Diagnostics are retained only in process memory
A workflow's diagnostic entries SHALL be retained only in the orchestrator's process memory for the lifetime of the owning workflow instance; no diagnostic entry SHALL be written to a persistent store, an external log shipper, or an external observability or tracing system by this capability.

#### Scenario: diagnostics unavailable after process restart
- **WHEN** the orchestrator process restarts
- **THEN** no diagnostic entry from a workflow run before the restart is retrievable

### Requirement: Render a workflow's diagnostic history on demand
The orchestrator SHALL provide a shell command that, given a workflow id, renders that workflow's retained diagnostic entries as text in chronological order, each showing its stage, attempt sequence number, duration, category, and reason.

#### Scenario: history rendered for a known workflow
- **WHEN** the diagnostics shell command is invoked with the id of a workflow that has recorded diagnostic entries
- **THEN** the command's output lists each retained entry in chronological order with its stage, attempt sequence number, duration, category, and reason

#### Scenario: history rendered for an unknown workflow
- **WHEN** the diagnostics shell command is invoked with an id that does not match any known workflow
- **THEN** the command reports that the workflow is unknown instead of rendering an empty or partial history
