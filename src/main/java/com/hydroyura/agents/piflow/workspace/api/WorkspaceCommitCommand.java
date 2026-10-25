package com.hydroyura.agents.piflow.workspace.api;

import java.util.List;

/**
 * Команда локального коммита версионируемых артефактов.
 *
 * @param workflowId идентификатор Workflow
 * @param repositoryUrl адрес репозитория
 * @param message сообщение коммита
 * @param relativePaths пути файлов относительно рабочей директории
 */
public record WorkspaceCommitCommand(
		String workflowId,
		String repositoryUrl,
		String message,
		List<String> relativePaths) {

	public WorkspaceCommitCommand {
		relativePaths = relativePaths == null ? List.of() : List.copyOf(relativePaths);
	}

	/**
	 * Проверяет обязательные поля команды коммита.
	 *
	 * @throws WorkspaceException если входные данные невалидны
	 */
	public void validate() {
		requireText(workflowId, "workflowId");
		requireText(repositoryUrl, "repositoryUrl");
		requireText(message, "message");
	}

	private static void requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw WorkspaceException.validation(field + " must not be blank");
		}
	}
}
