газ# Design

## Context

Existing architecture docs live in `docs/` (`system-overview.md`, `workflow.md`, `agent-roles.md`) and are written in English; `README.md` states goals but no run instructions. The end-to-end workflow has been exercised against the real `pi` CLI, so the runbook can describe verified behavior rather than assumptions.

## Goals / Non-Goals

**Goals:**
- A single, precise, reproducible runbook (`docs/howto.md`) for running and verifying the orchestrator.
- Accurate prerequisite lists for both the machine and the target repository, grounded in the verified run.

**Non-Goals:**
- No change to workflow behavior, stages, or agent roles.
- No documentation of multi-user/remote deployment (already out of scope per README).
- No persistence or scaling documentation.

## Decisions

1. **File: `docs/howto.md`, in English.** Keeps documentation together with the existing architecture docs and follows the project's English-docs convention.
   - Alternative rejected: expanding `README.md` in place — README stays a high-level overview; the runbook needs more space.
   - Alternative rejected: a root `HOWTO.md` — `docs/` is the established location.

2. **Target-repo prerequisite = existing Git repository.** Verified: the architect stage runs `openspec init` in the target repo, and agent skills/system prompts are bundled in the orchestrator or loaded globally. Therefore the runbook states the target repo only needs to be an existing local Git repository.
   - Alternative rejected: requiring a pre-initialized `openspec/` or project-local `.pi/skills/` — adds setup that the verified behavior does not require.

3. **Model configuration documented via `pi-flow.agents.*`, referenced (not duplicated) from `agent-roles.md`.** The runbook points to `application.yaml` as the single place to map tiers→models and roles→tiers.
   - Alternative rejected: hard-coding a model id in the runbook — model ids are environment-specific.

4. **Interactive REPL is the primary run path.** The runbook documents `java -jar target/pi-flow-0.0.1-SNAPSHOT.jar` and the shell commands; non-interactive mode is noted only with its current limitations.
   - Rationale: workflow state lives in an in-memory repository, so a full run must happen in one process; the interactive shell is the intended usage.

## Risks / Trade-offs

- [Runbook drifts as workflow stages/roles are provisional] → Mitigation: cross-reference `workflow.md`/`agent-roles.md` and keep the command walkthrough minimal; add a "stages are provisional" note.
- [Model/provider specifics are environment-dependent] → Mitigation: document the tier table as the single config point and instruct the user to use a model id valid for their configured provider.
- [Non-interactive mode has limitations (multi-word args split; JVM may not exit cleanly after a Pi run)] → Mitigation: document interactive mode as primary and note the limitations explicitly.
- [Transient agent failures mark the workflow FAILED with no auto-retry] → Mitigation: document re-running `flow-start` as the recovery step.
