package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Контракт activity независимого ревью Reviewer внутри Temporal execution.
 */
@ActivityInterface
public interface WorkflowReviewerActivities {

	/**
	 * Запускает Reviewer и возвращает текст результата ревью.
	 *
	 * @param input входные данные ревью
	 * @return результат ревью
	 */
	@ActivityMethod
	WorkflowReviewerResult review(WorkflowReviewerInput input);
}
