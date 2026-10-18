# Tasks

## 1. Configuration

- [x] 1.1 Add `AgentProperties` (`@ConfigurationProperties(prefix = "pi-flow.agents")`) with `tiers` and `roles` maps, and register it; verify a unit test binds a sample `tiers`/`roles` configuration.
- [x] 1.2 Add the `pi-flow.agents` section (tiers + roles for analyst/architect/developer/reviewer) to `application.yaml`; verify the application context loads.

## 2. Agent resources

- [x] 2.1 Add `src/main/resources/agents/{analyst,architect,developer,reviewer}/system.md`, each with a `tools` frontmatter and a role system-prompt body; verify the files exist at the expected classpath locations.
- [x] 2.2 Add at least one example skill as `agents/<role>/skills/*.md`; verify it is discoverable on the classpath.

## 3. Domain and resolver

- [x] 3.1 Rework `AgentDefinition` to carry name, resolved model, tools, system prompt, and skill resource paths; verify the project compiles.
- [x] 3.2 Add `ResourceAgentResolver` that reads `system.md` and `skills/*.md` from the classpath and resolves the model via `tiers`/`roles`; remove `FileAgentResolver` and the agent-file scanning, and update `AgentIntegrationConfig`; verify unit tests cover resolution, tier mapping, missing role/tier, and skill discovery.

## 4. Invocation

- [x] 4.1 Update `JsonModePiRunner` to use the resolved model, materialize each skill to a temp file, and pass `--skill` per skill; verify existing runner tests still pass.
- [x] 4.2 Add a test with a fake Pi asserting the runner passes `--model`, `--append-system-prompt`, and `--skill` for an agent that has a skill; verify that test passes.

## 5. Documentation

- [x] 5.1 Update `docs/agent-roles.md` to describe the config-driven provisioning (tiers/roles in `application.yaml`, per-role `system.md` and `skills/` resources); verify it stays consistent with `docs/system-overview.md`.

## 6. Validation

- [x] 6.1 Run `./mvnw test` and verify the full suite is green.
- [x] 6.2 Run `openspec validate 08-agent-provisioning` and verify the change is valid.
