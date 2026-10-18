# Proposal

## Why

pi-flow has no operational guide: a user must reverse-engineer how to build, run, and drive the orchestrator from source, tests, and the README's high-level goals. The prerequisites (JDK 27, `pi` in JSON mode, `openspec` CLI, `git`, model configuration) and the exact shell command sequence are currently undocumented, which makes the project hard to run and verify.

## What Changes

- Add `docs/howto.md` — a runbook for building, running, and verifying the orchestrator end to end.
- Document machine prerequisites: JDK 27, `pi` CLI (JSON mode), `openspec` CLI, `git`, a configured model provider with API key, and network access.
- Document target-repository prerequisites: an existing local Git repository; OpenSpec is initialized by the architect stage, so no pre-setup is required in the target repo.
- Document model/tier configuration (`pi-flow.agents.*` in `application.yaml`) and the `pi-flow.agent.*` overrides.
- Document the shell command set and the exact command sequence to drive a workflow through every stage and gate, with expected stage transitions.
- Document a verification checklist tied to observable artifacts at each gate.
- Link the runbook from `README.md`.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None.

This is a documentation-only change: it does not alter system behavior, so no spec delta is introduced (`skip_specs: true` in `.openspec.yaml`).

## Impact

- Documentation: new `docs/howto.md`; a link added in `README.md`.
- No code, API, dependency, or behavior changes.
- The documented prerequisites are runtime requirements, not new build dependencies.
