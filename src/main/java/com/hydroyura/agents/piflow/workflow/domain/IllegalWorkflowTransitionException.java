package com.hydroyura.agents.piflow.workflow.domain;

/**
 * Доменная ошибка недопустимого перехода.
 */
public class IllegalWorkflowTransitionException extends RuntimeException {

	public IllegalWorkflowTransitionException(String message) {
		super(message);
	}
}
