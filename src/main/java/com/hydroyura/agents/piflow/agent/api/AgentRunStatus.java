package com.hydroyura.agents.piflow.agent.api;

/**
 * Технический статус выполнения запуска агента.
 */
public enum AgentRunStatus {

	RUNNING,

	SUCCEEDED,

	FAILED,

	TIMED_OUT,

	CANCELLED
}
