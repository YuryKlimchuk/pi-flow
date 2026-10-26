package com.hydroyura.agents.piflow.workspace.api;

/**
 * Типизированная ошибка операций модуля workspace.
 */
public class WorkspaceException extends RuntimeException {

	/** Категория ошибки workspace. */
	public enum ErrorType {
		VALIDATION,
		REPOSITORY_ACCESS,
		FILESYSTEM,
		GIT,
		CORRUPT,
		DIRTY,
		LOCK_BUSY,
		RESULT
	}

	private final ErrorType errorType;

	public WorkspaceException(ErrorType errorType, String message) {
		super(message);
		this.errorType = errorType;
	}

	public WorkspaceException(ErrorType errorType, String message, Throwable cause) {
		super(message, cause);
		this.errorType = errorType;
	}

	public static WorkspaceException validation(String message) {
		return new WorkspaceException(ErrorType.VALIDATION, message);
	}

	public static WorkspaceException repositoryAccess(String message, Throwable cause) {
		return new WorkspaceException(ErrorType.REPOSITORY_ACCESS, message, cause);
	}

	public static WorkspaceException filesystem(String message, Throwable cause) {
		return new WorkspaceException(ErrorType.FILESYSTEM, message, cause);
	}

	public static WorkspaceException git(String message, Throwable cause) {
		return new WorkspaceException(ErrorType.GIT, message, cause);
	}

	public static WorkspaceException corrupt(String message) {
		return new WorkspaceException(ErrorType.CORRUPT, message);
	}

	public static WorkspaceException dirty(String message) {
		return new WorkspaceException(ErrorType.DIRTY, message);
	}

	public static WorkspaceException lockBusy(String message) {
		return new WorkspaceException(ErrorType.LOCK_BUSY, message);
	}

	public static WorkspaceException result(String message) {
		return new WorkspaceException(ErrorType.RESULT, message);
	}

	public ErrorType errorType() {
		return errorType;
	}
}
