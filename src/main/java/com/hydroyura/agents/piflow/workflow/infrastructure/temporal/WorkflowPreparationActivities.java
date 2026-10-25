package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Контракт activity подготовки workspace внутри Temporal execution.
 */
@ActivityInterface
public interface WorkflowPreparationActivities {

	/**
	 * Подготавливает локальную копию репозитория и рабочую ветку.
	 *
	 * @param input входные данные исполнения
	 * @return результат подготовки
	 */
	@ActivityMethod
	WorkflowPreparationResult prepare(WorkflowInput input);
}
