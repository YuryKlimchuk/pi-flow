package com.hydroyura.agents.piflow.agent.api;

/**
 * Типизированная ошибка операций модуля agent.
 */
public class AgentException extends RuntimeException {

	/** Категория ошибки запуска агента. */
	public enum ErrorType {
		PREPARATION,
		LAUNCH,
		EXECUTION,
		RESULT
	}

	private final ErrorType errorType;

	public AgentException(ErrorType errorType, String message) {
		super(message);
		this.errorType = errorType;
	}

	public AgentException(ErrorType errorType, String message, Throwable cause) {
		super(message, cause);
		this.errorType = errorType;
	}

	public static AgentException preparation(String message) {
		return new AgentException(ErrorType.PREPARATION, message);
	}

	public static AgentException launch(String message, Throwable cause) {
		return new AgentException(ErrorType.LAUNCH, message, cause);
	}

	public static AgentException execution(String message, Throwable cause) {
		return new AgentException(ErrorType.EXECUTION, message, cause);
	}

	public static AgentException result(String message, Throwable cause) {
		return new AgentException(ErrorType.RESULT, message, cause);
	}

	public ErrorType errorType() {
		return errorType;
	}
}
