package com.hydroyura.agents.piflow.workflow.application;

/**
 * Актуальное состояние длительного исполнения Workflow.
 *
 * @param phase фаза исполнения
 * @param workspacePath путь к подготовленному workspace при {@code SUCCEEDED}
 * @param branchName предложенное имя ветки при {@code SUCCEEDED}
 * @param errorMessage сообщение об ошибке при {@code FAILED}
 */
public record WorkflowExecutionState(
		WorkflowExecutionPhase phase,
		String workspacePath,
		String branchName,
		String errorMessage) {

	public static WorkflowExecutionState running() {
		return new WorkflowExecutionState(WorkflowExecutionPhase.RUNNING, null, null, null);
	}

	public static WorkflowExecutionState succeeded(String workspacePath, String branchName) {
		return new WorkflowExecutionState(WorkflowExecutionPhase.SUCCEEDED, workspacePath, branchName, null);
	}

	public static WorkflowExecutionState failed(String errorMessage) {
		return new WorkflowExecutionState(WorkflowExecutionPhase.FAILED, null, null, errorMessage);
	}
}
