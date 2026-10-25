package com.hydroyura.agents.piflow.workflow.application;

/**
 * Команда запуска длительного исполнения Workflow через application-порт.
 *
 * @param workflowId идентификатор workflow
 * @param repositoryUrl адрес репозитория
 * @param issueNumber номер задачи GitHub
 */
public record WorkflowExecutionCommand(String workflowId, String repositoryUrl, long issueNumber) {
}
