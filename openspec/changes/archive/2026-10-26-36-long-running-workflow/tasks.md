# Tasks

## 1. Public contracts and domain state

- [x] 1.1 Replace stage-specific execution commands with a stable workflow-id execution contract exposing start, query state, continue and rework operations; verify `workflow.api` compiles without Temporal imports.
- [x] 1.2 Extend the public workflow state with current role/action label, artifact metadata and available actions while preserving domain stage values; verify API serialization and mapping tests pass.
- [x] 1.3 Implement domain gates for Analyst and Architect, including valid `continue`/`rework` transitions, feedback validation, idempotent rejection of invalid commands and explicit unsupported Developer/Reviewer transitions; verify domain transition tests cover success and rejection cases.
- [x] 1.4 Update `WorkflowService` to create one domain record, attach one stable execution id, query the long-running execution and send signals instead of starting stage-specific executions; verify service tests prove Continue/Rework do not call start a second time.

## 2. Single Temporal workflow

- [x] 2.1 Define one Temporal workflow interface with `run`, `continueStage`, `reworkStage(feedback)` signals and `state` query, and remove adapter selection by `WorkflowExecutionKind`; verify Temporal interface and adapter unit tests compile and expose one workflow type.
- [x] 2.2 Implement durable sequential orchestration for preparation and named Analyst/Architect activities with `Workflow.await` gates; verify a workflow test observes one execution id across Analyst, Continue and Rework.
- [x] 2.3 Implement compact workflow state and signal validation so signals are accepted only at the current manual gate and duplicate/out-of-order signals do not invoke an activity twice; verify signal race and invalid-stage tests.
- [x] 2.4 Configure stable workflow id start, query handling and typed translation of Temporal start/query/signal failures; verify adapter tests cover duplicate start, unavailable Temporal and query failure.

## 3. Activities, roles and artifact boundaries

- [x] 3.1 Rename or add activity contracts to `prepareWorkspace`, `analyzeRequirements`, `designSolution`, `implementSolution` and `reviewImplementation` while keeping Developer/Reviewer unimplemented; verify architecture tests keep orchestration out of `agent`.
- [x] 3.2 Preserve Analyst validation for non-empty `analyst.md` and valid `branch.txt`, and return only workspace path plus serializable metadata; verify activity tests reject missing, empty and invalid artifacts.
- [x] 3.3 Implement Architect design artifact validation for `.pi-flow/design/architect.md` and role resources, with full text read only through workspace operations; verify Architect activity tests and payload assertions contain no full artifact text.
- [x] 3.4 Add explicit role/action metadata for Analyst, Architect, Developer and Reviewer and verify each agent invocation executes only the requested role with agent module contract tests.

## 4. Web and observable state

- [x] 4.1 Update controller handling to retain the same workflow id for start, track, continue and rework and to map typed query/signal failures; verify MVC tests for all endpoints.
- [x] 4.2 Update `workflow-status.html` to show current role/action, artifact result and Continue/Rework only at `WAITING_FOR_USER`, including Analyst and Architect gates; verify template tests cover running, waiting, failed and unsupported-stage states.
- [x] 4.3 Add validation and user feedback for empty rework feedback without creating a new execution; verify the POST rework MVC scenario.

## 5. Integration and migration verification

- [x] 5.1 Update Temporal integration tests to execute preparation, Analyst gate, Continue, Architect gate and Rework under one workflow id, including worker restart/replay behavior; verify the integration suite passes.
- [x] 5.2 Add architecture tests proving `web → workflow.api`, Temporal SDK only in `workflow.infrastructure`, and agent only launches processes; verify `WorkflowArchitectureTest` passes.
- [x] 5.3 Run `./mvnw test` and `openspec validate`; verify all tests pass and the change validates without warnings that block implementation.
