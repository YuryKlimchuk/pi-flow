package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Входная модель Temporal workflow.
 *
 * @param workflowId идентификатор workflow
 * @param repositoryUrl адрес репозитория
 * @param issueNumber номер задачи GitHub
 */
public record WorkflowInput(String workflowId, String repositoryUrl, long issueNumber) {
}
