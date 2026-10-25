package com.hydroyura.agents.piflow.github.application;

import com.hydroyura.agents.piflow.github.api.GithubException;
import com.hydroyura.agents.piflow.github.domain.GithubRemoteException;

/**
 * Преобразует внутренние ошибки адаптера в публичные ошибки контракта.
 */
public final class GithubErrors {

	private GithubErrors() {
	}

	public static GithubException map(GithubRemoteException exception) {
		return switch (exception.errorType()) {
			case CONFIGURATION -> GithubException.configuration(exception.operation());
			case AUTHENTICATION -> GithubException.authentication(exception.operation(), exception.status());
			case NOT_FOUND -> GithubException.notFound(exception.operation(), exception.status());
			case CONFLICT -> GithubException.conflict(exception.operation(), exception.status());
			case VALIDATION -> GithubException.validation(exception.operation(), exception.status(), exception.getMessage());
			case RATE_LIMIT -> GithubException.rateLimit(exception.operation(), exception.status());
			case SERVER -> GithubException.server(exception.operation(), exception.status());
			case NETWORK -> GithubException.network(exception.operation(), exception.getCause());
		};
	}
}
