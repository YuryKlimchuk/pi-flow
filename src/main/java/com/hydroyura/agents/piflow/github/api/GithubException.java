package com.hydroyura.agents.piflow.github.api;

/**
 * Типизированная ошибка операций модуля GitHub.
 *
 * <p>Несёт название операции, категорию ошибки, HTTP-статус (если есть) и признак
 * повторяемости. Секреты (токены) в сообщения ошибок не включаются.</p>
 */
public class GithubException extends RuntimeException {

	/** Категория ошибки GitHub-операции. */
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

	public GithubException(String operation, ErrorType errorType, int status, boolean retryable, String message) {
		super(message);
		this.operation = operation;
		this.errorType = errorType;
		this.status = status;
		this.retryable = retryable;
	}

	public GithubException(String operation, ErrorType errorType, int status, boolean retryable, String message,
			Throwable cause) {
		super(message, cause);
		this.operation = operation;
		this.errorType = errorType;
		this.status = status;
		this.retryable = retryable;
	}

	public static GithubException configuration(String operation) {
		return new GithubException(operation, ErrorType.CONFIGURATION, 0, false,
				"GitHub operation '" + operation + "' is not configured: token is missing");
	}

	public static GithubException authentication(String operation, int status) {
		return new GithubException(operation, ErrorType.AUTHENTICATION, status, false,
				"GitHub operation '" + operation + "' failed with authentication error (HTTP " + status + ")");
	}

	public static GithubException notFound(String operation, int status) {
		return new GithubException(operation, ErrorType.NOT_FOUND, status, false,
				"GitHub operation '" + operation + "' failed: resource not found (HTTP " + status + ")");
	}

	public static GithubException conflict(String operation, int status) {
		return new GithubException(operation, ErrorType.CONFLICT, status, false,
				"GitHub operation '" + operation + "' failed with conflict (HTTP " + status + ")");
	}

	public static GithubException validation(String operation, int status, String message) {
		return new GithubException(operation, ErrorType.VALIDATION, status, false, message);
	}

	public static GithubException rateLimit(String operation, int status) {
		return new GithubException(operation, ErrorType.RATE_LIMIT, status, true,
				"GitHub operation '" + operation + "' failed with rate limit (HTTP " + status + ")");
	}

	public static GithubException server(String operation, int status) {
		return new GithubException(operation, ErrorType.SERVER, status, true,
				"GitHub operation '" + operation + "' failed with server error (HTTP " + status + ")");
	}

	public static GithubException network(String operation, Throwable cause) {
		return new GithubException(operation, ErrorType.NETWORK, 0, true,
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
