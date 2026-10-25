package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * Реализация Temporal workflow минимального запуска Workflow.
 *
 * <p>Вызывает activity подготовки и возвращает её результат. Полная оркестрация
 * этапов бизнес-процесса находится за пределами текущего изменения.</p>
 */
public class PiFlowWorkflowImpl implements PiFlowWorkflow {

	private final WorkflowPreparationActivities activities = Workflow.newActivityStub(
			WorkflowPreparationActivities.class,
			ActivityOptions.newBuilder()
					.setStartToCloseTimeout(Duration.ofMinutes(10))
					.build());

	@Override
	public String execute(WorkflowInput input) {
		return activities.prepare(input.workflowId(), input.repositoryUrl(), input.issueNumber());
	}
}
