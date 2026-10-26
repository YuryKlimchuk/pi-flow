package com.hydroyura.agents.piflow.workflow.domain;

/**
 * Исходный контекст задачи Workflow.
 *
 * @param repositoryUrl адрес репозитория задачи
 * @param owner владелец репозитория
 * @param repository имя репозитория
 * @param issueNumber номер задачи GitHub
 */
public record WorkflowContext(String repositoryUrl, String owner, String repository, long issueNumber) {

	public WorkflowContext {
		if (repositoryUrl == null || repositoryUrl.isBlank()) {
			throw new IllegalArgumentException("repositoryUrl must not be blank");
		}
		if (owner == null || owner.isBlank()) {
			throw new IllegalArgumentException("owner must not be blank");
		}
		if (repository == null || repository.isBlank()) {
			throw new IllegalArgumentException("repository must not be blank");
		}
		if (issueNumber <= 0) {
			throw new IllegalArgumentException("issueNumber must be positive");
		}
	}
}
