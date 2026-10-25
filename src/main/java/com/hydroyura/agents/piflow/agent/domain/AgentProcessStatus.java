package com.hydroyura.agents.piflow.agent.domain;

/**
 * Технический статус завершения процесса (внутренняя модель).
 */
public enum AgentProcessStatus {

	SUCCEEDED,

	FAILED,

	TIMED_OUT,

	CANCELLED
}
