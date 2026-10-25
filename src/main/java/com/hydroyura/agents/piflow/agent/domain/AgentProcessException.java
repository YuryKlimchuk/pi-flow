package com.hydroyura.agents.piflow.agent.domain;

/**
 * Типизированная ошибка исполнения процесса Pi (внутренняя модель).
 */
public class AgentProcessException extends RuntimeException {

	public enum ErrorType {
		LAUNCH,
		EXECUTION,
		RESULT
	}

	private final ErrorType errorType;

	public AgentProcessException(ErrorType errorType, String message, Throwable cause) {
		super(message, cause);
		this.errorType = errorType;
	}

	public static AgentProcessException launch(String message, Throwable cause) {
		return new AgentProcessException(ErrorType.LAUNCH, message, cause);
	}

	public static AgentProcessException execution(String message, Throwable cause) {
		return new AgentProcessException(ErrorType.EXECUTION, message, cause);
	}

	public static AgentProcessException result(String message, Throwable cause) {
		return new AgentProcessException(ErrorType.RESULT, message, cause);
	}

	public ErrorType errorType() {
		return errorType;
	}
}
