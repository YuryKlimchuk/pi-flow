# Tasks

## 1. Record the decision

- [x] 1.1 Record the selected Spring ↔ Pi interaction variant and its rationale in `docs/system-overview.md` (the pi-flow → Pi Coding Agent relationship) and `docs/workflow.md` (Open questions); verify both files name the chosen variant and no longer list communication as an open question.
- [x] 1.2 Update `openspec/config.yaml` context to reflect that the Spring ↔ agent communication mechanism is now decided (keep persistence unresolved); verify the file parses with `openspec list`.

## 2. Validation

- [x] 2.1 Run `openspec validate 01-spring-pi-integration` and verify it reports the change as valid.
- [x] 2.2 Review the updated docs in a Markdown renderer and verify tables and Mermaid diagrams display correctly.
