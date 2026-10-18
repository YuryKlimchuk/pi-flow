# Agent Roles

Reference mapping of pi-flow agent roles to workflow stages and model-cost tiers.

Agent definitions live in the orchestrator's resources: each role has a folder
`src/main/resources/agents/<role>/` containing `system.md` (tools in frontmatter,
system prompt in the body) and a `skills/` subfolder. Only the model is configured
externally, through cost tiers in `application.yaml` (`pi-flow.agents`).

## Roles

| Role | Stage | Responsibility | Model tier |
|---|---|---|---|
| Analyst | Project analysis | Inspect the project and produce findings for the next stage. Read-only. | cheap |
| Architect | Change proposal | Author and update OpenSpec change artifacts (proposal, design, specs). | medium |
| Developer | Implementation + corrections | Implement the approved change and apply corrections. | expensive |
| Reviewer (QA) | Review | Evaluate the implementation against the task, approved artifacts, and verification results. Read-only. | medium |

## Model tiers

The tier → model mapping lives in `application.yaml`:

```yaml
pi-flow:
  agents:
    tiers:
      cheap:     <model-id>
      medium:    <model-id>
      expensive: <model-id>
    roles:
      analyst:    cheap
      architect:  medium
      developer:  expensive
      reviewer:   medium
```

| Tier | Use |
|---|---|
| cheap | High-volume, read-only, low-risk work. |
| medium | Planning and review. |
| expensive | Core implementation. |

## Resources per role

```
src/main/resources/agents/
  analyst/system.md
  architect/system.md
  developer/system.md
  developer/skills/java-code-style.md
  reviewer/system.md
```

Each `system.md` carries the tool allowlist in its frontmatter and the role's system prompt in its body.

## How an agent is launched

Pi has no native `--agent` or sub-agent flag: the role name only selects which
resource folder to load. pi-flow assembles one Pi invocation per stage from the
role's resources plus the tier-mapped model.

| Source | Effect |
|---|---|
| `pi-flow.agents.tiers` + `roles` | `--model <resolved model id>` |
| `system.md` frontmatter (`tools`) | `--tools <tools>` |
| `system.md` body | `--append-system-prompt <temp file>` |
| `skills/*.md` | `--skill <temp file>` per skill |
| always | `--mode json --no-session` |
| workflow target repository | process working directory (`cwd`) |

Concrete invocation:

```
pi --mode json --no-session --model <model> --tools <tools> \
   --append-system-prompt <system.md> --skill <skill.md> ... <prompt>
```

launched with the workflow's target repository as the working directory, so the
agent reads and writes the project being worked on.

## Notes

- Roles are roles of the same Pi Coding Agent, not separate services.
- The concrete model per tier is configured in `application.yaml` (`pi-flow.agents.tiers`).
- Corrections reuse the Developer role.
