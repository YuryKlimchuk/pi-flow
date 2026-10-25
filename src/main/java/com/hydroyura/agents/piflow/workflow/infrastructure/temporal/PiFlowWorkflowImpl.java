package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * Реализация Temporal workflow минимального сценария: подготовка workspace
 * выполняется в activity до завершения исполнения.
 *
 * <p>Запуск Pi-агента и дальнейшие стадии процесса находятся за пределами
 * текущего изменения.</p>
 */
public class PiFlowWorkflowImpl implements PiFlowWorkflow {

	private final WorkflowPreparationActivities activities = Workflow.newActivityStub(
			WorkflowPreparationActivities.class,
			ActivityOptions.newBuilder()
					.setStartToCloseTimeout(Duration.ofMinutes(10))
					.build());

	@Override
	public String execute(WorkflowInput input) {
		WorkflowPreparationResult result = activities.prepare(input);
		return result.branch() == null ? "prepared:" + input.workflowId() : result.branch();
	}
}
