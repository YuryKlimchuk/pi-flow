package com.hydroyura.agents.piflow.agent.application;

import com.hydroyura.agents.piflow.agent.api.AgentException;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessException;

/**
 * Преобразует внутренние ошибки адаптера в публичные ошибки контракта.
 */
public final class AgentErrors {

	private AgentErrors() {
	}

	public static AgentException map(AgentProcessException exception) {
		return switch (exception.errorType()) {
			case LAUNCH -> AgentException.launch(exception.getMessage(), exception.getCause());
			case EXECUTION -> AgentException.execution(exception.getMessage(), exception.getCause());
			case RESULT -> AgentException.result(exception.getMessage(), exception.getCause());
		};
	}
}
