# Design

## Context

`02-spring-pi-interaction` established JSON-mode invocation; `03-agent-roles` defined the four roles. Pi represents agents as markdown files with YAML frontmatter (`name`, `description`, `tools`, `model`) and a system-prompt body, used by Pi's subagent extension. The extension launches an agent by spawning `pi --mode json --no-session --model <m> --tools <t> --append-system-prompt <file> "<task>"`. This change brings that pattern into pi-flow.

## Goals / Non-Goals

**Goals:**
- Resolve an agent by name from the agent directories.
- Invoke Pi with the resolved agent's model, tools, and system prompt.

**Non-Goals:**
- No agent creation/validation — agents are assumed to be configured in Pi.
- No session persistence decisions here (kept as an open question).
- No workflow engine yet.

## Decisions

- **D1 — Agent definition.** `AgentDefinition` record: `name`, `description`, `model` (nullable), `tools` (list), `systemPrompt` (body). The body is everything after the YAML frontmatter.
- **D2 — Frontmatter parsing.** Parse the `---`-delimited YAML frontmatter with SnakeYAML (already on the classpath via Spring Boot); the rest of the file is the system prompt.
- **D3 — Resolution locations.** Look up `<name>.md` in `.pi/agents/` (project) first, then `~/.pi/agent/agents/` (user). Project overrides user. Directories are injectable so tests use fixtures.
- **D4 — Invocation flags.** For an agent: `pi --mode json --model <model> --tools <tools> --append-system-prompt <tmpfile> <prompt>`, stdin closed (as in `02`). Omit `--model`/`--tools`/`--append-system-prompt` when absent.
- **D5 — API.** `PiRunner` gains `run(agentName, prompt)`; the existing `run(prompt)` remains (no agent). Missing agent → FAILURE with a message naming it.
- **D6 — Session.** Match the subagent pattern and use `--no-session` for now; revisit if stage-session persistence is needed (open question).

## Risks / Trade-offs

- [Frontmatter schema may vary] → tolerate missing optional fields; fail only when the file is absent.
- [Temp file for system prompt] → write to a temp file and delete after the run; keep the body out of the command line.
- [Agent dirs differ across environments] → injectable dirs; defaults cover the standard locations.

## Open Questions

- Session persistence: `--no-session` vs persisting stage sessions for debugging.
- Whether `run(prompt)` (no agent) should be removed once all stages use agents.
