# Design

## Context

pi-flow is a greenfield Spring Boot 4.1.1 + Spring Shell 4.0.2 application (Java 27), currently a bare skeleton. It orchestrates Pi Coding Agent sessions through an explicit, human-gated development workflow, and the project itself is developed via OpenSpec. This change establishes the base architecture as documentation. See proposal.md - Why for motivation.

## Goals / Non-Goals

**Goals:**
- Fix the set of system participants and their responsibilities.
- Outline the end-to-end workflow: stages, gates, and correction loop (provisional — may change).
- Establish integration boundaries (agent integration, artifact validation, persistence) without premature abstraction.

**Non-Goals:**
- No code implementation in this change (documentation only).
- No formal specs yet — deferred to follow-up changes (this change declares `skip_specs`).
- Multi-user operation, cloud execution, and a web UI (per README).

## Decisions

- **D1 — Participants.** Five participants: User, pi-flow orchestrator, Pi Coding Agent (one session per stage), Workflow state storage, and the target Git repository. OpenSpec is not a participant — it is the working approach the agent and orchestrator follow (change artifacts and their validation). Rationale: mirrors the README "Responsibilities" section. Each stage runs under a distinct workflow *role* of the Pi agent — roles are assumed to exist and to differ per stage, but the concrete role set is intentionally not fixed yet and is never a separate service.

- **D2 — Workflow shape (provisional).** The workflow progresses through stages from task setup and project analysis, to change proposal, validation & approval, implementation, automated verification, review, and completion, with corrections looping back as needed and human approval gates between the major phases. The exact set and number of stages is not final and may change. Rationale: README "Intended workflow", which itself notes the workflow may evolve.

- **D3 — Agent integration boundary.** The orchestrator delegates each stage's work to the Pi Coding Agent under a distinct role. How Spring Boot and the agent communicate is deliberately out of scope for this step; the design only asserts the boundary, keeping the participant set and workflow independent of the future mechanism.

- **D4 — OpenSpec as the working approach.** OpenSpec is the methodology, not a separate system: the agent authors OpenSpec change artifacts and the orchestrator validates them via the `openspec` CLI (`validate`, `list`, `status`) rather than re-parsing markdown. Rationale: reuse the tool as the single source of truth.

- **D5 — Persistence boundary.** A local store holds workflow state, stage outcomes, approval decisions, and failures — execution metadata, not copies of repository artifacts — behind a port so the technology (embedded DB vs file) can be chosen later.

- **D6 — Code structure.** Layered layout with clear boundaries around the workflow engine, agent integration, artifact validation, and persistence. Exact package layout is deferred to the implementation change.

## Risks / Trade-offs

- [Agent communication mechanism unresolved] → keep the participant set and workflow independent of it; the boundary is asserted, the mechanism is deferred to a later change.
- [Mermaid rendering in review tools] → keep diagrams simple; fall back to ASCII if needed.

## Open Questions

- **Agent roles** — the concrete set of per-stage roles is not fixed yet; only that distinct roles exist and differ per stage is assumed.
- **Spring ↔ agent communication** — the mechanism by which the orchestrator invokes and talks to Pi sessions is bracketed out for this step; to be decided in a later change.
- **Persistence technology** — embedded database vs file-based storage.
