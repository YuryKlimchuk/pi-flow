# Tasks

## 1. Result model & parser

- [x] 1.1 Add `PiRunStatus` (SUCCESS, FAILURE, TIMEOUT) and a `PiRunResult` record (status, finalText, diagnostics); verify a unit test covers the record's construction.
- [x] 1.2 Add `PiJsonlParser` that assembles `message_update` deltas, finalizes on `message_end`, marks completion on `agent_settled`, and marks failure on an error/aborted `agent_end`; verify unit tests pass with fixture JSONL (success, failure, delta assembly).

## 2. Process runner

- [x] 2.1 Add the `PiRunner` port interface and `PiProperties` (`@ConfigurationProperties(prefix = "pi-flow.agent")`: command, timeout, workingDirectory); verify a slice test binds the properties and defaults.
- [x] 2.2 Add `JsonModePiRunner` implementing `PiRunner` via `ProcessBuilder` in JSON mode, draining stdout/stderr concurrently and enforcing the timeout; verify integration tests against a fake `pi` script return the expected result for success and failure.

## 3. Test fixtures & suite

- [x] 3.1 Add a fake `pi` executable test fixture that emits deterministic JSONL for success and failure paths; verify the fixture runs standalone and prints the expected JSONL.
- [x] 3.2 Run `./mvnw test` and verify the full suite (unit + integration) is green.
