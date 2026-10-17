# Design

## Context

Spring Boot 4.1.1. Spring Modulith compatibility: 2.1.0 targets Boot 4.1 (2.0 targets Boot 4.0). Current code has two natural modules: `agent` (agent integration) and `shell` (user interaction). Workflow engine and persistence are future modules.

## Goals / Non-Goals

**Goals:**
- Add Modulith with the correct version and minimal starters.
- Turn existing packages into explicit modules.
- Reorganize `agent` into a four-layer module.
- Add verification so future violations are caught.

**Non-Goals:**
- No `workflow` / `persistence` modules yet.
- No runtime verification flag change (test-based only).
- No changes to the `shell` internals beyond marking it a module.

## Decisions

- **D1 — Version.** Spring Modulith 2.1.0 (Boot 4.1 line).
- **D2 — Artifacts.** `spring-modulith-bom` (dependencyManagement import) + `spring-modulith-starter-core` (compile) + `spring-modulith-starter-test` (test). No jdbc/jpa until persistence lands. ArchUnit is overridden to 1.5.1 (test) because Modulith 2.1.0 ships ArchUnit 1.4.2, which cannot read Java 27 class files (major version 71).
- **D3 — Modules.** `agent` and `shell`, each with `package-info.java` carrying `@ApplicationModule`.
- **D4 — `agent` four-layer layout.**

| Layer | Contents |
|---|---|
| `agent/api/` | `PiRunner` (port), `PiRunResult`, `PiRunStatus` |
| `agent/application/` | `AgentResolver` (port interface) |
| `agent/domain/` | `AgentDefinition` (core value type) |
| `agent/infrastructure/` | `JsonModePiRunner`, `FileAgentResolver`, `PiJsonlParser`, `AgentFileParser`, `PiProperties`, `AgentIntegrationConfig` |

- **D5 — `shell` depends only on `agent.api`.** `AgentCommands` imports `PiRunner` and `PiRunResult` from `agent/api/`.
- **D6 — Verification.** A plain JUnit test running `ApplicationModules.of(PiFlowApplication.class).verify()` (no Spring context). Runtime verification stays off (default).
- **D7 — Lombok.** Lombok 1.18.48 (Boot manages 1.18.46, which only supports JDK 26). Loggers use `@Slf4j`. The compiler plugin configures `annotationProcessorPaths` for Lombok because JDK 23+ no longer runs annotation processors from the classpath by default.

## Risks / Trade-offs

- [verify() may flag the new layout] → adjust exposure (move types to `api` or mark named interfaces) rather than weakening verification.
- [Package move churn] → keep the `PiRunner` contract stable; tests move to mirror production packages.

## Open Questions

- When to reorganize `shell` into layers (out of scope here).
- Whether future modules should be scaffolded empty or created when needed.
