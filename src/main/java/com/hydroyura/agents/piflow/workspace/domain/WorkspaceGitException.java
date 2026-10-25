package com.hydroyura.agents.piflow.workspace.domain;

/**
 * Типизированная ошибка локального Git (внутренняя модель).
 */
public class WorkspaceGitException extends RuntimeException {

	public enum ErrorType {
		REPOSITORY_ACCESS,
		FILESYSTEM,
		GIT,
		CORRUPT,
		DIRTY
	}

	private final ErrorType errorType;

	public WorkspaceGitException(ErrorType errorType, String message) {
		super(message);
		this.errorType = errorType;
	}

	public WorkspaceGitException(ErrorType errorType, String message, Throwable cause) {
		super(message, cause);
		this.errorType = errorType;
	}

	public static WorkspaceGitException repositoryAccess(String message, Throwable cause) {
		return new WorkspaceGitException(ErrorType.REPOSITORY_ACCESS, message, cause);
	}

	public static WorkspaceGitException filesystem(String message, Throwable cause) {
		return new WorkspaceGitException(ErrorType.FILESYSTEM, message, cause);
	}

	public static WorkspaceGitException git(String message, Throwable cause) {
		return new WorkspaceGitException(ErrorType.GIT, message, cause);
	}

	public static WorkspaceGitException corrupt(String message) {
		return new WorkspaceGitException(ErrorType.CORRUPT, message);
	}

	public static WorkspaceGitException dirty(String message) {
		return new WorkspaceGitException(ErrorType.DIRTY, message);
	}

	public ErrorType errorType() {
		return errorType;
	}
}
