package com.hydroyura.agents.piflow.workflow.application;

/**
 * Порт запуска и управления единым длительным исполнением Workflow.
 */
public interface WorkflowExecutionPort {

	WorkflowExecutionResult start(WorkflowExecutionCommand command);

	WorkflowExecutionState queryState(String executionId);

	void continueStage(String executionId);

	void reworkStage(String executionId, String feedback);
}
