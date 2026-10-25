package com.hydroyura.agents.piflow.workflow.application;

/**
 * Результат запуска длительного исполнения Workflow.
 *
 * @param executionId идентификатор исполнения
 */
public record WorkflowExecutionResult(String executionId) {
}
