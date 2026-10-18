# Design

## Context

See `proposal.md` — Why. Today `agent.api.PiRunner` exposes `run(String prompt)` and `run(String agentName, String prompt)`. `JsonModePiRunner.runCommand` sets the child process directory from the global `pi-flow.agent.working-directory` property only. `AgentStageRunner` builds a prompt that mentions `workflow.getTargetRepo()` as text but never sets the process directory, so Pi runs outside the target repository.

## Goals / Non-Goals

**Goals:**
- Add an optional per-run working directory to agent invocation.
- Make agent stages (`PROJECT_ANALYSIS`, `CHANGE_PROPOSAL`, `IMPLEMENTATION`, `REVIEW`) launch Pi inside `workflow.getTargetRepo()`.
- Preserve the existing global `pi-flow.agent.working-directory` as the fallback default.

**Non-Goals:**
- No validation that the target repository path exists or is a Git repository.
- No working directory for the prompt-only `pi-run` shell command (manual use).
- No change to persistence or the workflow state machine.

## Decisions

- **D1 — Port surface.** Add `PiRunner.run(String agentName, String prompt, String workingDirectory)` and remove the two-argument `run(String agentName, String prompt)`: once `AgentStageRunner` switches to the three-argument form, the two-argument overload has no production caller. Keep `run(String prompt)` for the manual `pi-run` shell command.

  *Alternative considered:* keep `run(String agentName, String prompt)` as a default method delegating with a `null` directory. Rejected: it leaves dead API surface; the two test call sites are trivially updated to the three-argument form.

- **D2 — Directory type.** Use `String` for the working directory, matching `PiProperties.workingDirectory()` and `Workflow.getTargetRepo()` (both `String`).

  *Alternative considered:* `java.nio.file.Path`. Rejected: passing `null` is the "no explicit directory" signal, and `Path` is awkward as a nullable config value; `String` is converted to `Path` only at the `ProcessBuilder` boundary.

- **D3 — Precedence.** In `JsonModePiRunner`, resolve the effective directory as: explicit per-run value → else `properties.workingDirectory()` → else the process's own working directory (no `directory()` call). An explicit value always wins.

- **D4 — Threading.** `run(String prompt)` keeps the existing no-agent command path and passes `null`. The directory is applied in `runCommand`, which gains a `workingDirectory` parameter.

- **D5 — Stage runner.** `AgentStageRunner` passes `workflow.getTargetRepo()` as the working directory for every delegated agent stage. Stages with no agent mapping are unaffected (they return early, as today).

## Risks / Trade-offs

- [Nonexistent target repo path] → `ProcessBuilder.start()` throws `IOException`, which `JsonModePiRunner` already converts into a `FAILURE` result with diagnostics. Acceptable; explicit pre-validation is a possible follow-up.
- [Relative paths] → a relative `targetRepo` is resolved against the pi-flow process working directory. A local single-user tool normally receives an absolute path; documented as expected behavior.
- [Global config now secondary] → the global `working-directory` still applies when no per-run value is given, so existing behavior is preserved.

## Open Questions

- Whether the prompt-only `pi-run` shell command should later gain a `--dir` option.
