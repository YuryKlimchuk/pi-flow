package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Контракт Temporal workflow этапа Architect.
 */
@WorkflowInterface
public interface PiFlowArchitectWorkflow {

	@WorkflowMethod
	WorkflowAnalysisResult execute(WorkflowInput input);
}
