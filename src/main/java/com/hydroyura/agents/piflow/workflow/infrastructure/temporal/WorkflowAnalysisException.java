package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Типизированная ошибка activity анализа проекта.
 */
public class WorkflowAnalysisException extends RuntimeException {

	/** Причина отказа анализа. */
	public enum Reason {
		AGENT_FAILED,
		INVALID_RESULT
	}

	private final Reason reason;

	public WorkflowAnalysisException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public static WorkflowAnalysisException agentFailed(String message) {
		return new WorkflowAnalysisException(Reason.AGENT_FAILED, message);
	}

	public static WorkflowAnalysisException invalidResult(String message) {
		return new WorkflowAnalysisException(Reason.INVALID_RESULT, message);
	}

	public Reason reason() {
		return reason;
	}
}
