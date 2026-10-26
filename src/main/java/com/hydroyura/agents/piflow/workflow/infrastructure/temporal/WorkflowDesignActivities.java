package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Контракт activity проектирования (Architect) внутри Temporal execution.
 */
@ActivityInterface
public interface WorkflowDesignActivities {

	@ActivityMethod
	WorkflowAnalysisResult design(WorkflowDesignInput input);
}
