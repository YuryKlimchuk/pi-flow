# How to Run and Verify the Orchestrator

This runbook describes how to build, run, and verify **pi-flow** end to end. It is a hands-on guide; the architecture and the conceptual workflow are described in [`system-overview.md`](system-overview.md) and [`workflow.md`](workflow.md), and the role/stage mapping in [`agent-roles.md`](agent-roles.md).

> The workflow stages are provisional and may change. This guide reflects the current implementation.

## 1. Machine prerequisites

Install and configure the following on the machine that runs the orchestrator:

| Requirement | Version / notes | Why it is required |
|---|---|---|
| JDK | 27 | The build enforces Java 27 (`pom.xml`, Maven toolchain). |
| Maven | not required directly | The Maven Wrapper (`./mvnw`) is committed and downloads Maven on first run (needs network). |
| `pi` CLI | 1.3.0+ | Each stage launches `pi --mode json`. Must be on `PATH`. |
| Pi model provider + API key | configured in `~/.pi/agent/models.json` | Agents need a working model, e.g. `deepseek/deepseek-v4-pro` with `DEEPSEEK_API_KEY`. |
| `openspec` CLI | on `PATH` | The Architect stage runs `openspec init` / `new change` / `validate` inside the target repository. |
| `git` | on `PATH` | The target repository is a Git repository; agents may invoke `git`. |
| Network access | to the LLM provider | Agent stages call the configured model provider. |

Verify the two CLIs:

```sh
pi --version
openspec --help
```

## 2. Model configuration

Model tiers and roles are configured in `src/main/resources/application.yaml`:

```yaml
pi-flow:
  agents:
    tiers:
      cheap:     <model-id>
      medium:    <model-id>
      expensive: <model-id>
    roles:
      analyst:   cheap
      architect: medium
      developer: expensive
      reviewer:  medium
```

- `tiers` maps a tier name to a model id that must be valid for your configured Pi provider.
- `roles` maps each agent role to a tier. See [`agent-roles.md`](agent-roles.md) for the role/stage mapping.
- Optional overrides live under `pi-flow.agent.*`: `command` (default `pi`), `timeout` (default `5m`), `workingDirectory` (optional default working directory for agent runs).
- `pi-flow.verification.config-path` (default `.pi-flow/verify.json`) is the only startup-bound verification setting; it configures *where to look* for the per-target-repository verification file, not what to run. The verification command, timeout, and working directory are read fresh from that file on every verification attempt, so they can be changed at runtime without restarting the orchestrator.
- Retry and correction budgets live under `pi-flow.retry.*`: `max-technical-retries` (default `3`) bounds technical-failure attempts per stage, and `max-content-corrections` (default `5`) bounds content-correction loops per workflow. Both are commented out in `application.yaml` by default; auto-retry is disabled and there is no configuration to enable unattended retry.
- Diagnostics bounds live under `pi-flow.diagnostics.*`: `max-entries-per-workflow` (default `200`) caps the diagnostic entries retained per workflow, and `max-reason-length` (default `500`) caps each entry's reason string. These are in-memory reporting bounds only; the full attempt history is still available via `flow-status <id>`'s stage outcomes.

## 3. Target repository prerequisites

The target repository is the project the workflow operates on. It only needs to be an **existing local Git repository**:

```sh
git init /path/to/target-repo    # or use any existing repository
```

- It may be empty or contain code; the Analyst inspects whatever is there.
- **No OpenSpec setup is required beforehand.** The Architect stage runs `openspec init` and creates the `openspec/` directory inside the target repository.
- Agent skills and system prompts are bundled with the orchestrator or loaded from the global Pi configuration — they do **not** need to be placed in the target repository.
- **Verification configuration file** (optional until the verification stage): to run a deterministic check at the `VERIFICATION` stage, place a JSON file at `.pi-flow/verify.json` inside the target repository (path overridable via `pi-flow.verification.config-path`):

  ```json
  {
    "command": ["./gradlew", "check"],
    "timeoutSeconds": 300,
    "workingDirectory": "/optional/path"
  }
  ```

  `command` is a non-empty array of strings, `timeoutSeconds` is a positive integer, and `workingDirectory` is optional (defaults to the target repository root). The file is re-read fresh on every verification attempt; if it is missing the workflow pauses at `VERIFICATION` (`AWAITING_USER`) and can be retried with `flow-retry` after the file is added — no restart required. If it is present but invalid, the command cannot be started, or the command times out, the workflow pauses at `VERIFICATION` (`PAUSED`) as a technical failure and is retried with `flow-retry`.

## 4. Build and run

```sh
./mvnw package
java -jar target/pi-flow-0.0.1-SNAPSHOT.jar
```

The jar starts an interactive Spring Shell. Run `help` to see the available commands:

```
AVAILABLE COMMANDS
Agent Commands
	pi-run: Run a prompt against Pi in JSON mode
Built-In Commands
	clear, help, quit/exit, script, version
Diagnostics Commands
	flow-diagnostics: Show a workflow's recorded stage-attempt diagnostics
Flow Commands
	flow-approve: Approve the proposal, or accept the final result
	flow-reject:  Reject the proposal (cancels the workflow)
	flow-revise:  Request revisions to the proposal
	flow-rework:  Request further work after review
	flow-retry:   Retry a paused stage, or re-check verification configuration after a NOT_CONFIGURED pause
	flow-start:   Start a workflow for a task against a target repository
	flow-status:  Show a workflow's current stage and status
```

> Workflow state is kept **in memory**, so a full run must happen in a single shell session. Exiting the shell discards running workflows.

## 5. Drive a workflow end to end

Start a workflow with a task description and the target repository path:

```
flow-start "Add a LICENSE file" /path/to/target-repo
```

This runs the first stages automatically and pauses at the proposal gate:

1. **Task setup** — records the task and the repository.
2. **Project analysis** (Analyst) — inspects the repository and returns findings.
3. **Change proposal** (Architect) — runs `openspec init` and authors OpenSpec change artifacts in the target repository.
4. **Validation & approval** — the orchestrator pauses and prints the workflow id (`stage=VALIDATION_APPROVAL`, `status=AWAITING_USER`).

The `flow-start` output ends with lines like:

```
id=<uuid>
stage=VALIDATION_APPROVAL
status=AWAITING_USER
```

Copy the `id` for the following commands.

### Review the proposal, then approve

Inspect the generated artifacts in the target repository (`openspec/changes/<name>/`). Then decide:

```
flow-approve <id>   # proceed to implementation
flow-revise <id>    # send the proposal back to the Architect
flow-reject <id>    # cancel the workflow
```

### Implementation, verification, and review

`flow-approve <id>` runs the next stages automatically and pauses at the acceptance gate:

5. **Implementation** (Developer) — applies the change in the target repository.
6. **Automated verification** (Orchestrator) — runs the deterministic check.
7. **Review** (Reviewer) — evaluates the implementation.
8. **Acceptance** — the orchestrator pauses (`stage=ACCEPTANCE`, `status=AWAITING_USER`).

Check the result, then accept or request further work:

```
flow-approve <id>   # accept → COMPLETED
flow-rework <id>    # send it back to Implementation
```

After the final `flow-approve`, the workflow reaches `stage=COMPLETED`, `status=COMPLETED`.

## 6. Verification checklist

| Point | What to check | Where |
|---|---|---|
| After `flow-start` | OpenSpec is initialized | `openspec/` directory appears in the target repository |
| After `flow-start` | Change artifacts are authored | `openspec/changes/<name>/` (proposal, design, tasks, spec) |
| After first `flow-approve` | Implementation files exist | the file(s) the task asked for in the target repository |
| After first `flow-approve` | Review verdict returned | last stage outcome via `flow-status <id>` |
| After second `flow-approve` | Workflow completed | `flow-status <id>` shows `status=COMPLETED` |
| At any point | Diagnostic history for a workflow | `flow-diagnostics <id>` lists each retained stage attempt (stage, attempt, duration, category, reason) |

At any point, `flow-status <id>` shows the current `stage` and `status`, and `flow-diagnostics <id>` shows the bounded per-stage-attempt diagnostic record.

## 7. Known limitations

- **Single session per workflow** — state is in-memory; keep the shell open for the whole run.
- **Transient agent failures** — a technical failure pauses the workflow at `PAUSED` (or marks it `FAILED` once the technical-retry budget is exhausted); recover with `flow-retry` (adding `--acknowledge-partial-changes` when retrying `CHANGE_PROPOSAL` or `IMPLEMENTATION`). There is no automatic retry.
- **Non-interactive mode** — launching with `--spring.shell.interactive.enabled=false` is limited: multi-word arguments are split on whitespace, and the process may not exit cleanly after a Pi run. Prefer the interactive shell.
- **Stages are provisional** — stage names and order may change; see [`workflow.md`](workflow.md).
