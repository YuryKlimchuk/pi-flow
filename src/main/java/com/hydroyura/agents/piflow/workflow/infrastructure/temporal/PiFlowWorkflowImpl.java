package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * Реализация Temporal workflow: подготовка workspace и анализ проекта
 * выполняются в activity последовательно; успешное завершение возможно
 * только после валидного результата анализа.
 *
 * <p>Workflow не выполняет Git-операции и не запускает Pi напрямую.</p>
 */
public class PiFlowWorkflowImpl implements PiFlowWorkflow {

	private static final RetryOptions RETRY_OPTIONS = RetryOptions.newBuilder()
			.setMaximumAttempts(3)
			.setInitialInterval(Duration.ofSeconds(1))
			.setBackoffCoefficient(2.0)
			.setMaximumInterval(Duration.ofSeconds(10))
			.build();

	private final WorkflowPreparationActivities preparationActivities = Workflow.newActivityStub(
			WorkflowPreparationActivities.class,
			ActivityOptions.newBuilder()
					.setStartToCloseTimeout(Duration.ofMinutes(10))
					.setRetryOptions(RETRY_OPTIONS)
					.build());

	private final WorkflowAnalysisActivities analysisActivities = Workflow.newActivityStub(
			WorkflowAnalysisActivities.class,
			ActivityOptions.newBuilder()
					.setStartToCloseTimeout(Duration.ofMinutes(30))
					.setRetryOptions(RETRY_OPTIONS)
					.build());

	@Override
	public WorkflowAnalysisResult execute(WorkflowInput input) {
		WorkflowPreparationResult prepared = preparationActivities.prepare(input);
		return analysisActivities.analyze(new WorkflowAnalysisInput(
				input.workflowId(),
				input.repositoryUrl(),
				input.owner(),
				input.repository(),
				input.issueNumber(),
				prepared.path()));
	}
}
