# deterministic-verification Specification

## Purpose

Defines the per-target-repository, freshly-re-read-on-every-attempt configuration file for the external-command deterministic verification check, its four-valued result, the distinction between a command failure and an infrastructure error (including the exit-code-vs-timeout split), and the bounded, truncatable report it produces.

## Requirements

### Requirement: Verification configuration is a per-target-repository file, not a startup-bound setting
The orchestrator SHALL NOT assume, generate, or default to any specific build tool or command (for example Maven) when performing deterministic verification. The command (an argument vector), an optional working directory override, and a timeout SHALL be supplied entirely through a user-authored configuration file located within the workflow's target repository, at a path determined by a configurable location setting. The orchestrator SHALL NOT accept a command, timeout, or working directory from any other source, including agent-produced output.

#### Scenario: configured command is used as-is
- **WHEN** the target repository's verification configuration file specifies the command `["./gradlew", "check"]`
- **THEN** the verifier runs exactly that argument vector against that repository and does not substitute or append any other command

#### Scenario: no command is ever invented
- **WHEN** no verification configuration file exists for a target repository
- **THEN** the verifier does not invoke any process on behalf of that repository

#### Scenario: agent output is never treated as verification configuration
- **WHEN** an agent stage's output text contains something that resembles a command or verification configuration
- **THEN** the verifier does not execute it and continues to read its configuration only from the configuration file

### Requirement: Verification configuration is re-read fresh on every attempt, with no restart required to take effect
The verifier SHALL read the target repository's verification configuration file from disk on every verification attempt, including every retry, without caching its contents across attempts. A configuration file that is created, edited, or corrected while the application is running SHALL be observed by the very next verification attempt for that target repository, with no application restart required.

#### Scenario: a newly created configuration file is observed on the next attempt without a restart
- **WHEN** a verification attempt reports `NOT_CONFIGURED` because no configuration file exists, and a valid configuration file is then created for that target repository while the application keeps running
- **THEN** the next verification attempt for that workflow reads and uses the newly created file, with no restart of the application

#### Scenario: a corrected configuration file is observed on the next attempt without a restart
- **WHEN** a verification attempt reports `INFRASTRUCTURE_ERROR` because the configuration file was malformed, and the file is then corrected while the application keeps running
- **THEN** the next verification attempt for that workflow reads and uses the corrected file, with no restart of the application

### Requirement: Configuration file schema is fixed and strictly validated
The verification configuration file SHALL be a JSON object containing `command` (a non-empty array of strings), `timeoutSeconds` (a positive integer), and optionally `workingDirectory` (a string, defaulting to the target repository root when absent). A file that does not parse as JSON, is not a JSON object, is missing `command` or `timeoutSeconds`, has `command` present but empty, or has `timeoutSeconds` present but not a positive integer, SHALL be treated as a configuration error distinct from "no configuration file."

#### Scenario: well-formed configuration file is accepted
- **WHEN** the configuration file is `{"command": ["./check.sh"], "timeoutSeconds": 60}`
- **THEN** the verifier runs `./check.sh` with a 60 second timeout and the target repository as its working directory

#### Scenario: configuration file with a working directory override is honored
- **WHEN** the configuration file includes `"workingDirectory": "/tmp/alt-checkout"`
- **THEN** the verifier runs the configured command with `/tmp/alt-checkout` as its working directory instead of the target repository path

#### Scenario: malformed JSON is an infrastructure error, not NOT_CONFIGURED
- **WHEN** the configuration file exists but does not parse as JSON
- **THEN** the verifier reports `INFRASTRUCTURE_ERROR`, not `NOT_CONFIGURED` and not `PASSED`/`FAILED`

#### Scenario: empty command array is an infrastructure error
- **WHEN** the configuration file is `{"command": [], "timeoutSeconds": 60}`
- **THEN** the verifier reports `INFRASTRUCTURE_ERROR`

#### Scenario: non-positive timeout is an infrastructure error
- **WHEN** the configuration file's `timeoutSeconds` is zero, negative, or not an integer
- **THEN** the verifier reports `INFRASTRUCTURE_ERROR`

### Requirement: Verification runs as a bounded child process
When a valid verification configuration is read for a workflow's target repository, the verifier SHALL run the configured command as a child process using the configured argument vector, with its working directory set to the configured override when present or to the target repository path otherwise, and SHALL enforce the configured timeout by terminating the process if it does not finish in time.

#### Scenario: timeout terminates a hanging command
- **WHEN** the configured verification command does not exit within the configured timeout
- **THEN** the verifier terminates the process and reports an `INFRASTRUCTURE_ERROR` result, not `FAILED`

### Requirement: Verification result distinguishes a command failure from an infrastructure error
The verifier SHALL report exactly one of four outcomes for a verification attempt: `PASSED`, `FAILED`, `NOT_CONFIGURED`, or `INFRASTRUCTURE_ERROR`. `PASSED` SHALL be reported only when a validly configured command is actually started and exits with status code 0 within the configured timeout. `FAILED` SHALL be reported only when a validly configured command is actually started, runs to completion within the configured timeout, and exits with a nonzero status code; a `FAILED` result therefore always means the configured check itself ran validly and reported a content problem. `NOT_CONFIGURED` SHALL be reported when no verification configuration file exists for the workflow's target repository. `INFRASTRUCTURE_ERROR` SHALL be reported when a configuration file exists but is invalid per the schema requirement above, when a validly configured command cannot be started at all (for example the executable cannot be found, the process cannot be started due to a permissions error, or the configured working directory does not exist), or when a validly configured command is started but does not complete within the configured timeout. `INFRASTRUCTURE_ERROR` SHALL NOT be reported as `FAILED`, and `FAILED` SHALL NOT be reported as `INFRASTRUCTURE_ERROR`: the distinguishing fact is whether the configured command was actually started and ran to completion within its timeout, such that its own exit code could be observed. A timeout, exactly like a failure to start the process at all, means the command's exit behavior was never observed, and SHALL be classified as `INFRASTRUCTURE_ERROR`, not `FAILED`.

#### Scenario: passing command yields PASSED
- **WHEN** the configured verification command is started and exits with status code 0 within the timeout
- **THEN** the verifier reports `PASSED`

#### Scenario: failing command yields FAILED
- **WHEN** the configured verification command is started, completes within the configured timeout, and exits with a nonzero status code
- **THEN** the verifier reports `FAILED`, not `INFRASTRUCTURE_ERROR`

#### Scenario: timing-out command yields INFRASTRUCTURE_ERROR, not FAILED
- **WHEN** the configured verification command is started but does not exit within the configured timeout
- **THEN** the verifier reports `INFRASTRUCTURE_ERROR`, not `FAILED`, because the command's own exit code was never observed

#### Scenario: unconfigured verification yields NOT_CONFIGURED
- **WHEN** no verification configuration file exists for the workflow's target repository
- **THEN** the verifier reports `NOT_CONFIGURED`, not `PASSED`, `FAILED`, or `INFRASTRUCTURE_ERROR`

#### Scenario: a command that cannot be started yields INFRASTRUCTURE_ERROR, not FAILED
- **WHEN** the configuration file is valid but names an executable that does not exist, or a working directory that does not exist, so the process cannot be started at all
- **THEN** the verifier reports `INFRASTRUCTURE_ERROR`, not `FAILED` and not `NOT_CONFIGURED`

### Requirement: Verification report is bounded and flags truncation
The verifier SHALL capture the configured command's output (stdout and stderr) and exit code into a report, up to a fixed maximum size. When the captured output exceeds that bound, the verifier SHALL truncate the report and SHALL explicitly flag that truncation occurred in the result, rather than silently dropping the excess output without indication.

#### Scenario: report within bound is not flagged as truncated
- **WHEN** the configured command's output does not exceed the report size bound
- **THEN** the verification result's report contains the full output and is not flagged as truncated

#### Scenario: report exceeding bound is truncated and flagged
- **WHEN** the configured command's output exceeds the report size bound
- **THEN** the verification result's report is truncated to that bound and is explicitly flagged as truncated
