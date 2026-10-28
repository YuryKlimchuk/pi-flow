package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Контракт activity реализации решения Developer внутри Temporal execution.
 */
@ActivityInterface
public interface WorkflowDeveloperActivities {

	/**
	 * Запускает Developer и валидирует результат сборки и тестов.
	 *
	 * @param input входные данные реализации
	 * @return результат реализации с диагностикой
	 */
	@ActivityMethod
	WorkflowDeveloperResult implement(WorkflowDeveloperInput input);
}
