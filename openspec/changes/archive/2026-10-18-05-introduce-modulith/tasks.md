# Tasks

## 1. Dependencies

- [x] 1.1 Add `spring-modulith-bom:2.1.0` to dependencyManagement and `spring-modulith-starter-core` + `spring-modulith-starter-test` dependencies; verify `./mvnw dependency:tree` resolves Modulith 2.1.0.
- [x] 1.2 Add Lombok 1.18.48 (JDK 27 support), configure `annotationProcessorPaths` in the compiler plugin, and switch loggers to `@Slf4j`; verify `./mvnw test` is green.

## 2. Reorganize the agent module

- [x] 2.1 Move `PiRunner`, `PiRunResult`, `PiRunStatus` to `agent/api/`; split `AgentResolver` into an interface in `agent/application/` and `FileAgentResolver` in `agent/infrastructure/`; move `AgentDefinition` to `agent/domain/`; move `JsonModePiRunner`, `PiJsonlParser`, `AgentFileParser`, `PiProperties`, `AgentIntegrationConfig` to `agent/infrastructure/`; verify `./mvnw compile` succeeds.
- [x] 2.2 Update `AgentCommands` to import `PiRunner` and `PiRunResult` from `agent.api`; verify compilation.

## 3. Modules & verification

- [x] 3.1 Add `package-info.java` with `@ApplicationModule` to `agent` and `shell`; verify compilation.
- [x] 3.2 Add a module-structure test running `ApplicationModules.of(PiFlowApplication.class).verify()`; verify it passes.
- [x] 3.3 Move test classes to mirror the new production packages and update imports; run `./mvnw test` and verify the full suite is green.
