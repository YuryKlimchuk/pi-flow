package com.hydroyura.agents.piflow.workspace.api;

/**
 * Публичный порт локальных Git-операций модуля workspace.
 */
public interface WorkspaceGitOperations {

	/**
	 * Выполняет локальный коммит версионируемых артефактов на текущей рабочей ветке.
	 *
	 * @param command параметры коммита
	 * @return результат коммита
	 */
	WorkspaceCommitResult commit(WorkspaceCommitCommand command);

	/**
	 * Публикует (push) локальную ветку в удалённый репозиторий {@code origin}.
	 *
	 * @param command параметры публикации
	 * @return результат публикации
	 */
	WorkspacePushResult push(WorkspacePushCommand command);
}
