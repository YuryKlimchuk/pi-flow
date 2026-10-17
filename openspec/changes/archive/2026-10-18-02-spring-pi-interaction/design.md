# Design

## Context

Changes `00-base-architecture` and `01-spring-pi-integration` established the architecture and selected JSON mode for Spring ↔ Pi communication. This change implements the first, minimal, testable Spring ↔ Pi interaction — a spike that proves the integration contract before the workflow engine builds on it. Pi CLI (1.3.0) is invoked as `pi --mode json "<prompt>"`; it emits newline-delimited JSON events (`message_update` deltas, `message_end`, `agent_end`, `agent_settled`). The project is Spring Boot 4.1.1 / Java 27, package `com.hydroyura.agents.piflow`.

## Goals / Non-Goals

**Goals:**
- Implement a minimal agent-integration port that runs Pi in JSON mode and returns a structured result.
- Prove the interaction with automated tests that do not depend on a live model.

**Non-Goals:**
- Not the full workflow engine or stage orchestration.
- No workflow-state persistence yet.
- No Spring Shell commands exposing the interaction.

## Decisions

- **D1 — Port + adapter.** `PiRunner` (port interface) + `JsonModePiRunner` (adapter) using `ProcessBuilder`. Rationale: hides process mechanics behind the agent-integration boundary from `00-base-architecture` (D3/D6) and enables substitution in tests.
- **D2 — JSON mode invocation.** Spawn `pi --mode json <prompt>`; working directory configurable (target repo); stdout parsed as JSONL; stderr captured as diagnostics. Rationale: matches the `01-spring-pi-integration` decision.
- **D3 — Configurable command.** `PiProperties` via `@ConfigurationProperties(prefix = "pi-flow.agent")` with `command`, `timeout`, and `workingDirectory`. Tests substitute a fake `pi` script. Rationale: makes the interaction testable without a live model.
- **D4 — Result model.** `PiRunStatus` (SUCCESS / FAILURE / TIMEOUT) and `PiRunResult` (status, finalText, diagnostics). Rationale: maps to the "stage outcomes" (success/failure) from `docs/workflow.md`.
- **D5 — Dedicated JSONL parser.** `PiJsonlParser` (pure, no Spring) using Jackson 3 (`tools.jackson.databind`): assemble `message_update` deltas, finalize on `message_end`, mark completion on `agent_settled`, and mark failure on an error/aborted `agent_end`. Rationale: unit-testable without a process boundary.
- **D6 — Testing strategy (pyramid).** Unit tests for the parser (fixture JSONL); integration tests for `JsonModePiRunner` against a fake `pi` script (full process boundary, no live model). A real-Pi smoke test is out of scope for now.
- **D7 — Conventions.** Constructor injection with Lombok `@RequiredArgsConstructor`, `@Slf4j`, records for result/DTO types, AssertJ in tests, `@ConfigurationProperties` for configuration.

## Risks / Trade-offs

- [JSONL event format may evolve across Pi versions] → parser isolated; tests pin fixture events.
- [Process may hang] → enforce a configurable timeout and destroy the process on expiry.
- [stdout/stderr deadlock] → drain both streams concurrently.
- [Real-Pi tests are environment-dependent] → fake script for CI; a real smoke test is deferred.

## Open Questions

- Whether to expose a Spring Shell command for manual runs (deferred).
- Real-Pi smoke-test strategy (deferred).
