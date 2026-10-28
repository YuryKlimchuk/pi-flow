package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Контракт activity финализации внутри Temporal execution.
 */
@ActivityInterface
public interface WorkflowFinalizationActivities {

	/**
	 * Создаёт итоговую ветку, коммитит изменения, публикует ветку и создаёт pull request.
	 *
	 * @param input входные данные финализации
	 * @return результат финализации
	 */
	@ActivityMethod
	WorkflowFinalizationResult finalize(WorkflowFinalizationInput input);
}
