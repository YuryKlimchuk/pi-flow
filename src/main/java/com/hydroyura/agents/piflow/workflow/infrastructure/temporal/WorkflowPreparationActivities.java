package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Контракт activity минимального запуска Workflow.
 */
@ActivityInterface
public interface WorkflowPreparationActivities {

	@ActivityMethod
	String prepare(String workflowId, String repositoryUrl, long issueNumber);
}
