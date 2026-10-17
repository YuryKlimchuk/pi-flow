# Proposal

## Why

Changes `00-base-architecture` and `01-spring-pi-integration` established the architecture and selected JSON mode for Spring ↔ Pi communication, but nothing yet proves the interaction actually works from a Spring Boot process. This change implements a minimal, testable Spring ↔ Pi interaction — a spike — so the integration contract is validated before the workflow engine is built on top of it.

## What Changes

- Add an agent-integration port (`PiRunner`) that invokes the Pi CLI in JSON mode and returns a structured result.
- Add JSONL parsing and a result model distinguishing SUCCESS / FAILURE / TIMEOUT.
- Add unit tests for the parser and integration tests against a fake `pi` command (no live model required).
- Introduce the `agent-invocation` capability with formal requirements.

## Capabilities

### New Capabilities

- `agent-invocation`: invoking the Pi Coding Agent in JSON mode and capturing structured stage outcomes.

### Modified Capabilities

None.

## Impact

- New main-source package for agent integration and new test sources.
- Adds `tools.jackson.core:jackson-databind` (Boot-managed Jackson 3) for JSONL parsing; no other new dependencies.
- Lays the integration contract the workflow engine will consume.
