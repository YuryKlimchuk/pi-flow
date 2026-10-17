# Agent Roles

Reference mapping of pi-flow agent roles to workflow stages and model-cost tiers.

Agent definitions (instructions, tools, model) live in Pi as agent files under `~/.pi/agent/agents/`. pi-flow selects an agent by name when launching a stage; the model-cost tier below indicates which class of model each role should use.

## Roles

| Role | Stage | Responsibility | Model tier |
|---|---|---|---|
| Analyst | Project analysis | Inspect the project and produce findings for the next stage. Read-only. | cheap |
| Architect | Change proposal | Author and update OpenSpec change artifacts (proposal, design, specs). | medium |
| Developer | Implementation + corrections | Implement the approved change and apply corrections. | expensive |
| Reviewer (QA) | Review | Evaluate the implementation against the task, approved artifacts, and verification results. Read-only. | medium |

## Model tiers

| Tier | Use |
|---|---|
| cheap | High-volume, read-only, low-risk work. |
| medium | Planning and review. |
| expensive | Core implementation. |

## Notes

- Roles are roles of the same Pi Coding Agent, not separate services.
- The concrete model per tier is configured in each Pi agent's `model` field.
- Corrections reuse the Developer role.
