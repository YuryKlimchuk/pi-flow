package com.hydroyura.agents.piflow.agent.api;

/**
 * Structured result of a single Pi agent run.
 *
 * @param status      outcome of the run
 * @param finalText   authoritative final assistant text
 * @param diagnostics failure reason or process stderr, empty on success
 */
public record PiRunResult(PiRunStatus status, String finalText, String diagnostics) {
}
