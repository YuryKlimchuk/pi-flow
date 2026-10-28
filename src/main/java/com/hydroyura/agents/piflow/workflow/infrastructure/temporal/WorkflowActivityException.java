package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Типизированная ошибка activity единого workflow (Developer, Reviewer, finalization).
 */
public class WorkflowActivityException extends RuntimeException {

	/** Причина отказа activity. */
	public enum Reason {
		AGENT_FAILED,
		INVALID_RESULT,
		ORCHESTRATION_FAILED
	}

	private final Reason reason;

	public WorkflowActivityException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public WorkflowActivityException(Reason reason, String message, Throwable cause) {
		super(message, cause);
		this.reason = reason;
	}

	public static WorkflowActivityException agentFailed(String message) {
		return new WorkflowActivityException(Reason.AGENT_FAILED, message);
	}

	public static WorkflowActivityException agentFailed(String message, Throwable cause) {
		return new WorkflowActivityException(Reason.AGENT_FAILED, message, cause);
	}

	public static WorkflowActivityException invalidResult(String message) {
		return new WorkflowActivityException(Reason.INVALID_RESULT, message);
	}

	public static WorkflowActivityException orchestrationFailed(String message) {
		return new WorkflowActivityException(Reason.ORCHESTRATION_FAILED, message);
	}

	public static WorkflowActivityException orchestrationFailed(String message, Throwable cause) {
		return new WorkflowActivityException(Reason.ORCHESTRATION_FAILED, message, cause);
	}

	public Reason reason() {
		return reason;
	}
}
