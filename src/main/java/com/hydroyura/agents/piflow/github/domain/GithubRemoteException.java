package com.hydroyura.agents.piflow.github.domain;

/**
 * Типизированная ошибка удалённого GitHub API (внутренняя модель).
 */
public class GithubRemoteException extends RuntimeException {

	public enum ErrorType {
		CONFIGURATION,
		AUTHENTICATION,
		NOT_FOUND,
		CONFLICT,
		VALIDATION,
		RATE_LIMIT,
		SERVER,
		NETWORK
	}

	private final String operation;
	private final ErrorType errorType;
	private final int status;
	private final boolean retryable;

	public GithubRemoteException(String operation, ErrorType errorType, int status, boolean retryable, String message) {
		super(message);
		this.operation = operation;
		this.errorType = errorType;
		this.status = status;
		this.retryable = retryable;
	}

	public GithubRemoteException(String operation, ErrorType errorType, int status, boolean retryable, String message,
			Throwable cause) {
		super(message, cause);
		this.operation = operation;
		this.errorType = errorType;
		this.status = status;
		this.retryable = retryable;
	}

	public static GithubRemoteException configuration(String operation) {
		return new GithubRemoteException(operation, ErrorType.CONFIGURATION, 0, false,
				"GitHub operation '" + operation + "' is not configured: token is missing");
	}

	public static GithubRemoteException authentication(String operation, int status) {
		return new GithubRemoteException(operation, ErrorType.AUTHENTICATION, status, false,
				"GitHub operation '" + operation + "' failed with authentication error (HTTP " + status + ")");
	}

	public static GithubRemoteException notFound(String operation, int status) {
		return new GithubRemoteException(operation, ErrorType.NOT_FOUND, status, false,
				"GitHub operation '" + operation + "' failed: resource not found (HTTP " + status + ")");
	}

	public static GithubRemoteException conflict(String operation, int status) {
		return new GithubRemoteException(operation, ErrorType.CONFLICT, status, false,
				"GitHub operation '" + operation + "' failed with conflict (HTTP " + status + ")");
	}

	public static GithubRemoteException validation(String operation, int status, String message) {
		return new GithubRemoteException(operation, ErrorType.VALIDATION, status, false, message);
	}

	public static GithubRemoteException rateLimit(String operation, int status) {
		return new GithubRemoteException(operation, ErrorType.RATE_LIMIT, status, true,
				"GitHub operation '" + operation + "' failed with rate limit (HTTP " + status + ")");
	}

	public static GithubRemoteException server(String operation, int status) {
		return new GithubRemoteException(operation, ErrorType.SERVER, status, true,
				"GitHub operation '" + operation + "' failed with server error (HTTP " + status + ")");
	}

	public static GithubRemoteException network(String operation, Throwable cause) {
		return new GithubRemoteException(operation, ErrorType.NETWORK, 0, true,
				"GitHub operation '" + operation + "' failed with network error", cause);
	}

	public String operation() {
		return operation;
	}

	public ErrorType errorType() {
		return errorType;
	}

	public int status() {
		return status;
	}

	public boolean retryable() {
		return retryable;
	}
}
