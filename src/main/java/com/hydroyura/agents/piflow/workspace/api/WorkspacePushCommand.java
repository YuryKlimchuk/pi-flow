package com.hydroyura.agents.piflow.workspace.api;

/**
 * Команда публикации (push) локальной ветки в удалённый репозиторий.
 *
 * @param workflowId идентификатор Workflow
 * @param repositoryUrl адрес репозитория
 * @param branch имя публикуемой ветки
 */
public record WorkspacePushCommand(String workflowId, String repositoryUrl, String branch) {

	public void validate() {
		if (workflowId == null || workflowId.isBlank()) {
			throw WorkspaceException.validation("workflowId must not be blank");
		}
		if (repositoryUrl == null || repositoryUrl.isBlank()) {
			throw WorkspaceException.validation("repositoryUrl must not be blank");
		}
		if (branch == null || branch.isBlank()) {
			throw WorkspaceException.validation("branch must not be blank");
		}
	}
}
