package com.hydroyura.agents.piflow.workflow.api;

/**
 * Типизированная ошибка операций модуля workflow.
 */
public class WorkflowException extends RuntimeException {

	/** Категория ошибки workflow. */
	public enum ErrorType {
		VALIDATION,
		NOT_FOUND,
		ILLEGAL_TRANSITION,
		ORCHESTRATION,
		EXECUTION
	}

	private final ErrorType errorType;

	public WorkflowException(ErrorType errorType, String message) {
		super(message);
		this.errorType = errorType;
	}

	public WorkflowException(ErrorType errorType, String message, Throwable cause) {
		super(message, cause);
		this.errorType = errorType;
	}

	public static WorkflowException validation(String message) {
		return new WorkflowException(ErrorType.VALIDATION, message);
	}

	public static WorkflowException notFound(String message) {
		return new WorkflowException(ErrorType.NOT_FOUND, message);
	}

	public static WorkflowException illegalTransition(String message) {
		return new WorkflowException(ErrorType.ILLEGAL_TRANSITION, message);
	}

	public static WorkflowException orchestration(String message, Throwable cause) {
		return new WorkflowException(ErrorType.ORCHESTRATION, message, cause);
	}

	public static WorkflowException execution(String message, Throwable cause) {
		return new WorkflowException(ErrorType.EXECUTION, message, cause);
	}

	public ErrorType errorType() {
		return errorType;
	}
}
