# Proposal

## Why

Agent definitions are currently scattered in `.md` files outside the orchestrator (`~/.pi/agent/agents/` and `.pi/agents/`), mixing model, tools, and system prompt in one place. The orchestrator should own agent provisioning: each role's system prompt and skills live in the orchestrator's resources, and only the model is resolved from configuration through cost tiers.

## What Changes

- Add configuration for cost tiers (`cheap`/`medium`/`expensive`) mapping a tier name to a concrete model id, and a role map assigning each agent a tier.
- Add per-agent resource folders under `src/main/resources/agents/<role>/` containing `system.md` (tools in frontmatter, system prompt in the body) and a `skills/` subfolder with `SKILL.md` skills.
- Replace the file-based agent resolver with a resource+config resolver that loads the system prompt and tools from `system.md`, resolves the model through the tier mapping, and discovers the agent's skills.
- Extend the Pi invocation to pass each discovered skill via `--skill` (materialized to a temp directory, like the system prompt).
- **BREAKING**: remove `FileAgentResolver`, `AgentFileParser`, and the scan of `~/.pi/agent/agents/` and `.pi/agents/`.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `agent-invocation`: agent definition resolution moves from external `.md` files to orchestrator resources plus config, and invocation gains tier-based model resolution and skill passing.

## Impact

- `agent` module: `AgentDefinition`, `AgentFileParser`, `FileAgentResolver`, and `AgentIntegrationConfig` are reworked or removed in favor of config+resource provisioning.
- New `@ConfigurationProperties` for `pi-flow.agents.tiers` and `pi-flow.agents.roles`.
- `JsonModePiRunner` resolves tier → model id, materializes `system.md` and skills, and passes `--model`, `--tools`, `--append-system-prompt`, and `--skill`.
- `application.yaml` gains the `pi-flow.agents` section; `src/main/resources/agents/` is added.
