# Spec Delta

## ADDED Requirements

### Requirement: Run in a specified working directory
The runner SHALL accept an optional working directory per run and launch the Pi process in that directory when one is provided.

#### Scenario: working directory provided
- **WHEN** the runner is invoked with a working directory
- **THEN** the Pi process is launched with that directory as its working directory

#### Scenario: no working directory provided
- **WHEN** the runner is invoked without a working directory
- **THEN** the Pi process is launched with the configured default working directory, or the process's own directory if none is configured
