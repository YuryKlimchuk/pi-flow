package com.hydroyura.agents.piflow.workflow.api;

/**
 * Категория статуса Workflow в публичном контракте.
 */
public enum WorkflowStatus {

	CREATED,

	RUNNING,

	WAITING_FOR_USER,

	COMPLETED,

	FAILED,

	CANCELLED
}
