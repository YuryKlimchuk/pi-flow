# Design

## Context

See `proposal.md` — Why. Today `agent.api.PiRunner` resolves agents through `FileAgentResolver` scanning `~/.pi/agent/agents/` and `.pi/agents/`, parsing `.md` files whose frontmatter carries model and tools and whose body is the system prompt. The new design makes the orchestrator own agent provisioning: per-role resources plus a thin config for models.

## Goals / Non-Goals

**Goals:**
- Per-role resource folders (`system.md` + `skills/`) under `src/main/resources/agents/`.
- Config-driven model selection through cost tiers (`cheap`/`medium`/`expensive`).
- Invocation passes the resolved model, tools, system prompt, and skills.

**Non-Goals:**
- No runtime-editable agent files outside the orchestrator resources.
- No skill assets (scripts/references) in this change — standalone `.md` skills only.
- No startup validation of the role/tier configuration.

## Decisions

- **D1 — Config shape.** A new `@ConfigurationProperties(prefix = "pi-flow.agents")` record:

  ```java
  record AgentProperties(Map<String, String> tiers, Map<String, String> roles) {}
  ```

  `tiers` maps a tier name to a model id; `roles` maps a role name to a tier name. Example `application.yaml`:

  ```yaml
  pi-flow:
    agents:
      tiers:
        cheap: deepseek/deepseek-v4-pro
        medium: anthropic/claude-sonnet-4
        expensive: anthropic/claude-opus-4
      roles:
        analyst: cheap
        architect: medium
        developer: expensive
        reviewer: medium
  ```

- **D2 — Resource layout.** Per role: `agents/<role>/system.md` (frontmatter `tools`; body = system prompt) and `agents/<role>/skills/*.md` (standalone skills). Example:

  ```
  src/main/resources/agents/
    analyst/system.md
    analyst/skills/recon.md
    developer/system.md
    developer/skills/java-code-style.md
    ...
  ```

- **D3 — Domain model.** Rework `AgentDefinition` to carry a resolved model and skills:

  ```java
  record AgentDefinition(String name, String model, List<String> tools, String systemPrompt, List<String> skills) {}
  ```

  `skills` holds the resource paths of the role's skills; `model` is resolved, not raw frontmatter.

- **D4 — Resolver.** Replace `FileAgentResolver` with a `ResourceAgentResolver` that, given a name, reads `agents/<name>/system.md` and `agents/<name>/skills/*.md` from the classpath, resolves the tier via `roles[name]` and the model via `tiers[tier]`, and returns a resolved `AgentDefinition`. Frontmatter parsing reuses the existing parser logic (now reading `system.md`). A missing role, missing tier, or missing `system.md` yields an empty result with a clear reason.

- **D5 — Invocation.** `JsonModePiRunner` keeps its shape but: the model comes from the resolved definition (tier → id); the system prompt is written to a temp file and passed via `--append-system-prompt`; each skill is materialized to a temp file and passed via `--skill <path>` (repeatable). Precedence of the working directory from change 07 is unchanged.

- **D6 — Skills form.** v1 supports standalone `.md` skills under `skills/`, materialized one file at a time and passed as `--skill`. Directory-form skills with bundled assets are deferred (see Open Questions).

- **D7 — Removal.** Delete `FileAgentResolver` and `AgentFileParser`'s agent-file scanning; repurpose the parser for `system.md`. `AgentIntegrationConfig` now wires `ResourceAgentResolver` with `AgentProperties`.

## Risks / Trade-offs

- [Skills with bundled assets unsupported] → v1 is instructions-only; directory-form skills are a follow-up.
- [Misconfigured tier/role] → resolver returns a failure with a diagnostic naming the unresolved tier or role.
- [Breaking change] → external `.md` agent files are no longer read; any existing `~/.pi/agent/agents/*.md` becomes irrelevant.

## Open Questions

- Whether to support directory-form skills (with `scripts/`, `references/`, `assets/`) by extracting whole skill directories to temp.
- Whether to validate, at startup, that every configured role has a matching resource folder and tier.
