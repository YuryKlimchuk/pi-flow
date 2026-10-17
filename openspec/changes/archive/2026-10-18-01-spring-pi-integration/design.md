# Design

## Context

`00-base-architecture` established the five participants and the workflow, but deliberately left the Spring ↔ agent communication mechanism open. Pi exposes several integration modes; all use the same agent, sessions, resources, and tools — the mode only changes the interface, lifetime, and control model. This change compares the modes from the perspective of a Java (Spring Boot) orchestrator and prepares a recommendation. The final choice is made by the user during review.

## Goals / Non-Goals

**Goals:**
- Enumerate every Pi integration mode and its trade-offs for pi-flow.
- Recommend an optimal variant.

**Non-Goals:**
- No implementation in this change.
- No final selection here — the user chooses during review.

## Evaluation criteria

- **Orchestration fit** — supports the explicit, reproducible, stage-per-session workflow from `docs/workflow.md`.
- **Result capture** — structured stage outcomes that the orchestrator can persist to workflow state.
- **Observability** — visibility into what the agent did (tool calls, progress, failures).
- **Complexity** — lifecycle management, parsing, and failure-handling burden in Java.
- **Java integration effort** — how naturally the mode fits a Spring Boot process.

## Options

| # | Mode | Interface | Lifetime | Control |
|---|---|---|---|---|
| 1 | Print | final text on stdout | one invocation | fire-and-forget |
| 2 | JSON | JSONL events on stdout | one invocation | fire-and-forget + structured stream |
| 3 | RPC | JSONL commands/events (stdin/stdout) | long-lived | bidirectional |
| 4 | Interactive TUI | terminal UI | until user exits | human-driven |
| 5 | SDK (TypeScript) | in-process API | host process | programmatic (Node/Bun) |

### 1. Print mode (`pi --print`)

One-shot run: final assistant text to stdout, then exit.

- **Pros:** simplest integration (spawn + read stdout); exit code signals failure; no protocol to parse.
- **Cons:** only final text — no intermediate events (tool calls, thinking); unstructured stage results; weak observability; failure detail limited to exit status and stderr.

### 2. JSON mode (`pi --mode json`)

One-shot run: a JSONL stream of agent/session events, then exit.

- **Pros:** structured events (messages, tool calls, `agent_end`/`agent_settled`) give precise, persistable stage outcomes; streamable progress; same simple spawn-per-stage model as print; good observability.
- **Cons:** must parse and assemble JSONL (deltas → authoritative `message_end`); no mid-run control (all prompts supplied up front); must inspect events for failure, not just the exit code; protocol versioning to track.

### 3. RPC mode (`pi --mode rpc`)

Long-lived subprocess controlled by JSONL commands on stdin; responses and events on stdout.

- **Pros:** full bidirectional control — multiple prompts, steer/follow-up, change model, inspect state, manage sessions; process reuse across a stage or across stages; language-independent.
- **Cons:** highest complexity — lifecycle, id correlation, backpressure, timeouts, reconnection, concurrency; stdout reserved for protocol records and must be continuously drained; more moving parts to fail; overkill when each stage is effectively one prompt.

### 4. Interactive TUI (`pi`)

Pi's full terminal UI, with the user working directly in each session.

- **Pros:** maximal human involvement per stage; full interactive UX (steering, approvals, slash commands); no integration code.
- **Cons:** not programmatic — the orchestrator cannot reliably capture or record results; the workflow becomes human-driven rather than orchestrator-driven, conflicting with the "explicit orchestration" principle; outcomes are hard to persist; not reproducible or scriptable.

### 5. SDK (TypeScript, in-process)

Embed Pi in a Node.js/Bun process via `@earendil-works/pi-coding-agent`.

- **Pros:** richest programmatic API (AgentSession, streaming events, session management, tools, models); typed; no process boundary inside the host.
- **Cons:** not Java-native — requires a Node.js/Bun sidecar process plus an interop bridge (another protocol to build and maintain); adds a Node runtime dependency; most moving parts; the SDK targets Node/Bun hosts, not Spring Boot.

## Recommendation

For pi-flow (Java, local-first, stage-per-session, explicit orchestration), **JSON mode (option 2)** is the recommended primary mechanism:

- it keeps the simple spawn-per-stage model of print mode, and
- it gives structured, persistable stage outcomes with good observability.

**Print mode (1)** is the minimal fallback when only the final text matters. **RPC mode (3)** is the upgrade path if we later need long-lived or multi-turn control. **Interactive TUI (4)** remains for exploratory/manual use, not the default orchestration path. **SDK (5)** is rejected for the initial version because it forces a Node sidecar.

> **Decision:** JSON mode (option 2) selected as the primary Spring ↔ Pi interaction mechanism.

## Risks / Trade-offs

- [JSONL event format may evolve across Pi versions] → isolate behind a single agent-integration port and pin/verify the Pi version.
- [Process lifecycle (timeouts, streams, exit codes)] → encapsulate in one component; deterministic checks stay outside the agent.
- [Mid-run control absent in JSON/print] → acceptable for one-shot stages; switch to RPC only if a stage genuinely needs multi-turn control.

## Open Questions

None — the variant is selected (JSON mode). If multi-turn control is ever needed, RPC can be revisited in a follow-up change.
