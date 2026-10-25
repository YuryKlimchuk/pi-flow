package com.hydroyura.agents.piflow.workflow.domain;

/**
 * Статус жизненного цикла Workflow.
 */
public enum WorkflowStatus {

	CREATED,

	RUNNING,

	WAITING_FOR_USER,

	COMPLETED,

	FAILED,

	CANCELLED
}
