package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Контракт activity анализа проекта внутри Temporal execution.
 */
@ActivityInterface
public interface WorkflowAnalysisActivities {

	/**
	 * Запускает Analyst и валидирует сохранённый артефакт анализа.
	 *
	 * @param input входные данные анализа
	 * @return путь и метаданные валидного артефакта
	 */
	@ActivityMethod
	WorkflowAnalysisResult analyze(WorkflowAnalysisInput input);
}
