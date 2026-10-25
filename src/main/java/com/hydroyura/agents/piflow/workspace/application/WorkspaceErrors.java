package com.hydroyura.agents.piflow.workspace.application;

import com.hydroyura.agents.piflow.workspace.api.WorkspaceException;
import com.hydroyura.agents.piflow.workspace.domain.LockBusyException;
import com.hydroyura.agents.piflow.workspace.domain.WorkspaceGitException;

/**
 * Преобразует внутренние ошибки адаптеров в публичные ошибки контракта.
 */
public final class WorkspaceErrors {

	private WorkspaceErrors() {
	}

	public static WorkspaceException map(WorkspaceGitException exception) {
		return switch (exception.errorType()) {
			case REPOSITORY_ACCESS -> WorkspaceException.repositoryAccess(exception.getMessage(), exception.getCause());
			case FILESYSTEM -> WorkspaceException.filesystem(exception.getMessage(), exception.getCause());
			case GIT -> WorkspaceException.git(exception.getMessage(), exception.getCause());
			case CORRUPT -> WorkspaceException.corrupt(exception.getMessage());
			case DIRTY -> WorkspaceException.dirty(exception.getMessage());
		};
	}

	public static WorkspaceException map(LockBusyException exception) {
		return WorkspaceException.lockBusy(exception.getMessage());
	}
}
