# Tasks

## 1. Agent definition & resolution

- [x] 1.1 Add `AgentDefinition` record (name, description, model, tools, systemPrompt) and a frontmatter parser (YAML frontmatter + body); verify a unit test parses a sample agent file.
- [x] 1.2 Add `AgentResolver` reading `.pi/agents/<name>.md` (project) then `~/.pi/agent/agents/<name>.md` (user), with injectable directories; verify a unit test resolves a fixture agent and reports a missing agent.

## 2. Agent-aware invocation

- [x] 2.1 Extend `PiRunner` with `run(agentName, prompt)` and `JsonModePiRunner` to pass `--model`, `--tools`, `--append-system-prompt` from the resolved agent (stdin still closed); verify integration tests with a fake agent + fake `pi` return the expected result.
- [x] 2.2 Keep `run(prompt)` working without an agent; verify existing tests still pass.

## 3. Validation

- [x] 3.1 Run `./mvnw test` and verify the full suite (unit + integration) is green.
- [x] 3.2 Run `openspec validate 04-agent-aware-invocation` and verify it reports the change as valid.
