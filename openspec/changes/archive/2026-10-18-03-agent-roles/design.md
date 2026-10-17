# Design

## Context

`00-base-architecture` deferred the concrete agent-role set. `docs/workflow.md` describes the stages where the agent acts: project analysis, change proposal, implementation, review, corrections. This change fixes the role set and maps each role to a model-cost tier (expensive / medium / cheap). Agent definitions themselves live in Pi (`~/.pi/agent/agents/*.md`); pi-flow only references an agent by name per stage.

## Goals / Non-Goals

**Goals:**
- Derive the minimal role set from the workflow.
- Define each role's responsibility and (conceptual) tooling.
- Map each role to a model tier with rationale and record it in a reference doc.

**Non-Goals:**
- No concrete model names or providers — tiers only.
- No code that consumes the roles yet.
- No Pi agent files created here — the user defines them in Pi.

## Roles derived from the workflow

| Role | Stage | Responsibility | Tooling (conceptual) | Model tier |
|---|---|---|---|---|
| Analyst | Project analysis | Inspect the project and produce findings for the next stage. | read-only (read, grep, find, ls) | cheap |
| Architect | Change proposal | Author/update OpenSpec change artifacts (proposal, design, specs). | read + write | medium |
| Developer | Implementation + corrections | Implement the approved change and apply corrections. | read + write + bash + edit | expensive |
| Reviewer (QA) | Review | Evaluate the implementation against the task, approved artifacts, and verification results. | read-only | medium |

## Model-tier mapping rationale

- **Analyst → cheap** — high-volume, read-only, exploratory work; a cheap model is sufficient.
- **Architect → medium** — planning quality matters, but it is a low-frequency step.
- **Developer → expensive** — the core code-authoring step; highest model quality yields the most value.
- **Reviewer (QA) → medium** — sound, independent judgment, cheaper than the developer.

## Reference file

`docs/agent-roles.md` records the mapping as a living reference (role → stage → responsibility → model tier). It is documentation, not application configuration.

The actual agent definitions live in Pi as agent files (frontmatter `name`, `description`, `tools`, `model` plus instructions). The user creates one Pi agent per role and sets its `model` according to the tier. When launching a stage, pi-flow selects the corresponding agent by name (the exact selection mechanism is resolved when the workflow engine is implemented).

## Risks / Trade-offs

- [Role set may evolve] → the reference doc is cheap to update; no code change.
- [Tier → concrete model mapping deferred] → tiers are stable categories; concrete models are resolved per Pi agent.
- [Reviewer misses issues] → promote Reviewer to expensive if reviews prove weak.

## Open Questions

- Whether **Reviewer (QA)** should be `medium` or `expensive`.
- Whether **corrections** reuse the Developer role (current assumption) or need a dedicated role.
- Whether **Architect** should be `medium` or `expensive`.
