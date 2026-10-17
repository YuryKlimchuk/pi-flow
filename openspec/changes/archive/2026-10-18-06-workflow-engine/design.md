# Design

## Context

`docs/workflow.md` defines the stages and gates; `docs/agent-roles.md` maps stages to roles; `agent.api.PiRunner` can run Pi under a role. This change adds the orchestration engine as a new `workflow` Modulith module. Execution is synchronous; state is persisted through a repository port with an in-memory default.

## Goals / Non-Goals

**Goals:**
- Hand-rolled enum state machine over the workflow stages.
- Repository port + in-memory Map implementation.
- Synchronous stage progression with gates and correction loops.
- Shell commands to drive the workflow.

**Non-Goals:**
- No async/background execution.
- No real persistence (DB) — in-memory only for now.
- No real automated verification commands yet (verification is a stub).

## Decisions

- **D1 — State machine.** Hand-rolled enum-based: `WorkflowStage` + `WorkflowStatus` + `Workflow` aggregate; transitions in a `WorkflowEngine`. No Spring Statemachine.
- **D2 — Stages.** `TASK_SETUP, PROJECT_ANALYSIS, CHANGE_PROPOSAL, VALIDATION_APPROVAL, IMPLEMENTATION, VERIFICATION, REVIEW, ACCEPTANCE, COMPLETED`. Terminal states via `WorkflowStatus` (IN_PROGRESS, AWAITING_USER, COMPLETED, CANCELLED, FAILED).
- **D3 — Repository.** `WorkflowRepository` port (`save`, `findById`, `findAll`) + `InMemoryWorkflowRepository` (ConcurrentHashMap).
- **D4 — Synchronous.** `WorkflowEngine.advance()` runs agent stages inline and returns when a gate is reached or the workflow ends.
- **D5 — Stage runner.** `StageRunner` port (`run(stage, workflow) -> StageOutcome`); `AgentStageRunner` maps stage → agent name → `PiRunner.run(agentName, prompt)`. Verification is an orchestrator stub for now.
- **D6 — Gates and loops.** Approve/reject at the proposal gate; corrections loop from verification/review back to implementation; accept/further-work at the acceptance gate.
- **D7 — Module layout.**

| Layer | Contents |
|---|---|
| `workflow/api/` | `WorkflowEngine` (port), `WorkflowStage`, `WorkflowStatus`, `StageOutcome`, `WorkflowView` |
| `workflow/application/` | `WorkflowEngineImpl`, `WorkflowRepository` (port), `StageRunner` (port) |
| `workflow/domain/` | `Workflow`, `WorkflowId` |
| `workflow/infrastructure/` | `InMemoryWorkflowRepository`, `AgentStageRunner`, config |

- **D8 — Shell.** `flow-start`, `flow-status`, `flow-approve`, `flow-reject` commands in `shell`, depending on `workflow.api`.

## State machine

```
TASK_SETUP -> PROJECT_ANALYSIS -> CHANGE_PROPOSAL -> VALIDATION_APPROVAL
                                                        |
                              approve ------------------+-> IMPLEMENTATION
                              reject  ------------------+-> CANCELLED
                              revisions ----------------+-> CHANGE_PROPOSAL (loop)

IMPLEMENTATION <----+
   |               | corrections
   v               |
VERIFICATION --fail-+
   | passed
   v
REVIEW --corrections--> (back to IMPLEMENTATION)
   | passed
   v
ACCEPTANCE --accept--> COMPLETED
   | further work --> IMPLEMENTATION (loop)
```

## Risks / Trade-offs

- [State machine complexity] → keep transitions explicit and unit-test every edge.
- [In-memory state lost on restart] → acceptable for the first iteration; DB persistence is a follow-up change.
- [Stage prompt quality] → prompts are minimal placeholders; refined later.

## Open Questions

- When to replace the verification stub with real configured checks.
- Stage → role mapping: hardcoded vs configurable.
