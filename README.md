# pi-flow

**pi-flow** is a local development workflow orchestrator built with Java and Spring Boot. It coordinates AI-assisted software development using Pi Coding Agent and OpenSpec.

The goal is to provide a reproducible, controlled development workflow in which each stage has a clear responsibility, produces explicit artifacts, and is validated before the process moves forward.

## Goals

- Orchestrate software development through a predefined sequence of stages.
- Integrate with Pi Coding Agent from a Java application.
- Use OpenSpec to define, review, and validate proposed changes before implementation.
- Preserve human control over important decisions and approval gates.
- Track workflow execution, stage results, and failures.
- Keep the architecture simple and extensible.

## Core principles

### Explicit orchestration

Spring Boot is responsible for workflow execution, stage transitions, validation, and approval gates. The workflow must not depend on an AI agent autonomously following the entire process.

### Separate agent sessions

Each workflow stage runs in a separate Pi Coding Agent session with a stage-specific role and instructions. Results from one stage are explicitly passed to subsequent stages.

### Human approval

The user reviews and approves the proposed changes before implementation begins. The user also retains final acceptance authority over the completed work.

### OpenSpec-driven development

OpenSpec artifacts define the proposed change and serve as the agreed reference for implementation and review.

### Local-first architecture

The initial version runs locally and operates on an existing Git repository selected by the user. Multi-user operation, remote deployment, and cloud execution are outside the initial scope.

### Extensibility without premature complexity

Integration boundaries should allow future changes to agent invocation, context preparation, workflow stages, and persistence without introducing unnecessary abstractions or infrastructure in the initial version.

## Intended workflow

1. **Task setup** — the user provides a task description and the path to an existing Git repository.
2. **Project analysis** — an Analyst stage examines the project and returns findings.
3. **Change proposal** — an Architect stage prepares the relevant OpenSpec artifacts.
4. **Validation and approval** — the orchestrator checks the expected artifacts and their validity. The user reviews the proposal and approves it or requests revisions.
5. **Implementation** — a Developer stage implements the approved change.
6. **Automated verification** — the orchestrator runs the configured tests and checks.
7. **Review** — a Reviewer stage evaluates the implementation against the task, approved OpenSpec artifacts, code changes, and verification results.
8. **Corrections** — when necessary, implementation and verification are repeated.
9. **Completion** — the user reviews the result, and the orchestrator records the workflow outcome.

The workflow may evolve as implementation details and practical limitations are discovered.

## Responsibilities

**Spring Boot orchestrator**
- Manages workflow state and stage transitions.
- Starts Pi Coding Agent sessions and collects their results.
- Performs deterministic checks and invokes configured validation commands.
- Enforces approval gates and records execution outcomes.

**Pi Coding Agent**
- Performs the work assigned to the current stage.
- Operates under stage-specific instructions and the context supplied by the orchestrator.
- Produces analysis, OpenSpec artifacts, implementation changes, or review findings as appropriate.

Analyst, Architect, Developer, and Reviewer are workflow roles, not separate system services.

**OpenSpec**
- Provides structured artifacts for proposing and describing changes.
- Supports the validation of change artifacts before implementation.

**User**
- Defines the task and selects the target repository.
- Reviews and approves the proposal.
- Makes final acceptance decisions.

**Target Git repository**
- Contains the application being changed.
- Is separate from the `pi-flow` repository itself.

## Initial scope

The first version focuses on a single-user, local workflow with explicit stage transitions, separate agent sessions, proposal approval, implementation, verification, and review.

The initial implementation should avoid unnecessary distributed components, microservices, and infrastructure. Technical choices should be made through the project's OpenSpec workflow rather than assumed in advance.

## Non-goals for the initial version

- A general-purpose multi-agent platform.
- Multi-user collaboration and access management.
- Remote or cloud-based execution.
- A mandatory web UI.
- Independent services for each agent role.
- Autonomous execution without human approval at defined gates.

## Development approach

The project itself is developed using Pi Coding Agent and OpenSpec.

Architecture decisions, functional requirements, and implementation plans should be documented and reviewed through the project's agreed development workflow. Implementation should begin only after the relevant proposal has been reviewed and approved.

## Status

Early-stage project. Architecture, requirements, and implementation details are subject to refinement.
