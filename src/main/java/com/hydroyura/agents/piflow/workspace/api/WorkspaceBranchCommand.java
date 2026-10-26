package com.hydroyura.agents.piflow.workspace.api;

/**
 * Команда создания итоговой ветки.
 *
 * @param workflowId идентификатор Workflow
 * @param repositoryUrl адрес репозитория
 * @param branch имя итоговой ветки
 */
public record WorkspaceBranchCommand(String workflowId, String repositoryUrl, String branch) {

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
