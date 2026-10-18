# Tasks

## 1. Prerequisites

- [x] 1.1 Write the machine-prerequisites section of `docs/howto.md` (JDK 27, `pi` CLI in JSON mode, `openspec` CLI, `git`, model provider + API key, network) and verify each listed item is actually required by cross-checking the verified end-to-end run.
- [x] 1.2 Write the target-repository-prerequisites section of `docs/howto.md` (existing local Git repository; OpenSpec initialized by the architect stage) and verify it matches the observed architect-stage behavior.

## 2. Build, run, and commands

- [x] 2.1 Document the build (`./mvnw package`) and run (`java -jar target/pi-flow-0.0.1-SNAPSHOT.jar`) steps and verify the jar builds and the shell starts with `help` listing the commands.
- [x] 2.2 Document the shell command set (`flow-start`, `flow-status`, `flow-approve`, `flow-reject`, `flow-revise`, `flow-rework`, `pi-run`) and verify `help` output matches the documented commands.

## 3. Workflow walkthrough and verification

- [x] 3.1 Document the end-to-end command sequence (`flow-start` → `flow-approve` → `flow-approve`) with the expected stage transitions and verify it by running the sequence against a scratch Git repository and observing the stages reach `COMPLETED`.
- [x] 3.2 Document a verification checklist for each gate (OpenSpec change artifacts after proposal; implementation files after approve; review verdict before final accept) and verify every checklist item maps to an observable output.

## 4. Link and consistency

- [x] 4.1 Add a link to `docs/howto.md` from `README.md` and verify the link renders.
- [x] 4.2 Cross-check `docs/howto.md` against `docs/system-overview.md`, `docs/workflow.md`, and `docs/agent-roles.md` and verify there are no contradictions in stage names, role names, or config keys.
