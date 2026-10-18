# Tasks

## 1. Extend the PiRunner port

- [x] 1.1 Add `PiRunResult run(String agentName, String prompt, String workingDirectory)` to `agent.api.PiRunner` and remove the now-redundant `run(String agentName, String prompt)` overload; verify the project compiles with `JsonModePiRunner` implementing the new method.
- [x] 1.2 Update the existing `JsonModePiRunnerTest` calls that used the two-argument form to the three-argument form; verify the suite passes.
- [x] 1.3 Add a unit test for the new overload that verifies the runner launches the Pi process with the provided working directory; verify that test passes.

## 2. Apply the directory in JsonModePiRunner

- [x] 2.1 Implement the new overload in `JsonModePiRunner` and thread a `workingDirectory` parameter through `runCommand`, resolving explicit value → global `pi-flow.agent.working-directory` → process default; verify the existing `JsonModePiRunnerTest` still passes.
- [x] 2.2 Add a test using a fake Pi that prints its own working directory, asserting the explicit directory wins over the configured global default; verify that test passes.

## 3. Run agent stages in the target repository

- [x] 3.1 Update `AgentStageRunner` to pass `workflow.getTargetRepo()` as the working directory to `PiRunner.run(agent, prompt, workingDirectory)`; update `AgentStageRunnerTest` to use a real temp directory as the target repository and a fake Pi that prints its own working directory, asserting the stage runs in that directory; verify the suite passes.

## 4. Documentation

- [x] 4.1 Update `docs/system-overview.md` (pi-flow → Pi Coding Agent relationship) to state that agent stages launch Pi inside the target repository; verify the wording is consistent with `docs/workflow.md`.

## 5. Validation

- [x] 5.1 Run `./mvnw test` and verify the full suite is green.
- [x] 5.2 Run `openspec validate 07-target-repo-working-dir` and verify the change is valid.
