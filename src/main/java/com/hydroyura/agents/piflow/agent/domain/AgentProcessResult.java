package com.hydroyura.agents.piflow.agent.domain;

/**
 * Технический результат исполнения процесса (внутренняя модель).
 */
public record AgentProcessResult(
		String runId,
		AgentProcessStatus status,
		String stdout,
		String stderr,
		Integer exitCode) {
}
