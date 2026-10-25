package com.hydroyura.agents.piflow.workflow.domain;

/**
 * Исходный контекст задачи Workflow.
 *
 * @param repositoryUrl адрес репозитория задачи
 * @param issueNumber номер задачи GitHub
 */
public record WorkflowContext(String repositoryUrl, long issueNumber) {

	public WorkflowContext {
		if (repositoryUrl == null || repositoryUrl.isBlank()) {
			throw new IllegalArgumentException("repositoryUrl must not be blank");
		}
		if (issueNumber <= 0) {
			throw new IllegalArgumentException("issueNumber must be positive");
		}
	}
}
