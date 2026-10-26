package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Контракт Temporal workflow миниального запуска Workflow.
 */
@WorkflowInterface
public interface PiFlowWorkflow {

	@WorkflowMethod
	WorkflowAnalysisResult execute(WorkflowInput input);
}
