# Tasks

## 1. Roles reference doc

- [x] 1.1 Create `docs/agent-roles.md` with the four roles (analyst, architect, developer, reviewer), their stages, responsibilities, and model tiers; verify the file contains a role → tier table.
- [x] 1.2 Update `docs/workflow.md` stage tables and per-step actions to use the defined role names (Analyst, Architect, Developer, Reviewer) instead of the generic "Agent"; verify the names appear on the corresponding stages.

## 2. Validation

- [x] 2.1 Run `openspec validate 03-agent-roles` and verify it reports the change as valid.
- [x] 2.2 Verify `docs/agent-roles.md` lists exactly four roles, each with a non-empty description and a tier in {expensive, medium, cheap}.
