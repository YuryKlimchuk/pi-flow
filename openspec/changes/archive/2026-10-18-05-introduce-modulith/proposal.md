# Proposal

## Why

pi-flow is growing: the agent integration and shell UI already form two distinct concerns, and the workflow engine and persistence are on the roadmap. Spring Modulith gives explicit module boundaries and verification, keeping the architecture honest as it grows — without the overhead of microservices. This change introduces Modulith and reorganizes the existing `agent` code into a proper four-layer module.

## What Changes

- Add Spring Modulith 2.1.0 (compatible with Spring Boot 4.1): BOM + starter-core + starter-test.
- Mark the existing `agent` and `shell` packages as application modules (`@ApplicationModule`).
- Reorganize `agent` into four layers: `api`, `application`, `domain`, `infrastructure`.
- Split `AgentResolver` into a port (interface) and a file-based adapter.
- Add a module-structure verification test (`ApplicationModules.verify()`).
- Add Lombok and switch loggers to `@Slf4j`.

## Capabilities

### New Capabilities

None — infrastructure/refactoring change, no user-observable behavior.

### Modified Capabilities

None.

## Impact

- `pom.xml`: new BOM import and two dependencies.
- `agent` package reorganized; `shell` package marked as a module.
- Production and test sources moved across packages; `PiRunner` contract unchanged.
