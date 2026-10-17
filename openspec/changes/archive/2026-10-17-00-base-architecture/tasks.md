# Tasks

## 1. Architecture documentation

- [x] 1.1 Create `docs/system-overview.md` describing the five participants (User, pi-flow orchestrator, Pi Coding Agent, Workflow state storage, Target repository), their responsibilities, and relationships; verify the file exists and includes a `flowchart` Mermaid diagram of participants and links.
- [x] 1.2 Create `docs/workflow.md` describing the workflow stages, a `sequenceDiagram` Mermaid flow with each participant's actions, per-step action details, and open questions; verify the file exists and includes a `sequenceDiagram` Mermaid diagram.

## 2. Validation

- [x] 2.1 Run `openspec validate 00-base-architecture` and verify it reports the change as valid.
- [x] 2.2 Review both documents in a Markdown/Mermaid renderer and verify tables and Mermaid diagrams display correctly; fix any diagram syntax that fails to render.
