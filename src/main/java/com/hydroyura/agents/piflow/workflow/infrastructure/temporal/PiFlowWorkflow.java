package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Единый длинный Temporal workflow всего флоу разработки.
 *
 * <p>Одно execution на пользовательский workflow; ручные этапы управляются
 * сигналами {@code continueStage} и {@code reworkStage}, текущее состояние
 * читается query {@code state}.</p>
 */
@WorkflowInterface
public interface PiFlowWorkflow {

	@WorkflowMethod
	void run(WorkflowInput input);

	@SignalMethod
	void continueStage();

	@SignalMethod
	void reworkStage(String feedback);

	@QueryMethod
	WorkflowStageState state();
}
