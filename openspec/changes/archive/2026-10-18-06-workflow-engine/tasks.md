# Tasks

## 1. Domain model

- [x] 1.1 Add `WorkflowStage`, `WorkflowStatus`, `StageOutcome`, `WorkflowId`, and the `Workflow` aggregate; verify a unit test covers workflow creation and stage-result recording.

## 2. Repository

- [x] 2.1 Add the `WorkflowRepository` port and `InMemoryWorkflowRepository` (Map-backed); verify a unit test covers save/findById/findAll.

## 3. State machine

- [x] 3.1 Add `StageRunner` port and `WorkflowEngine` (impl in application) that advances stages synchronously, stops at gates, and applies approve/reject/correction/final-acceptance transitions; verify unit tests with a fake stage runner cover the happy path, rejection, and correction loops.

## 4. Agent stage runner

- [x] 4.1 Add `AgentStageRunner` mapping stage → agent name and calling `PiRunner.run(agentName, prompt)`; verify an integration test with a fake Pi returns the expected stage outcome.

## 5. Shell commands & module wiring

- [x] 5.1 Add `package-info.java` with `@ApplicationModule` to `workflow`, and expose `workflow.api` via `@NamedInterface("api")`; verify the Modulith structure test still passes.
- [x] 5.2 Add `flow-start`, `flow-status`, `flow-approve`, `flow-reject` commands in `shell` using `WorkflowEngine`; verify the app context loads.

## 6. Validation

- [x] 6.1 Run `./mvnw test` and verify the full suite is green.
- [x] 6.2 Run `openspec validate 06-workflow-engine` and verify valid.
