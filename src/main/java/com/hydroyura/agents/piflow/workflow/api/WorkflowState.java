package com.hydroyura.agents.piflow.workflow.api;

import java.util.List;

/**
 * Публичное состояние Workflow.
 *
 * @param workflowId идентификатор workflow
 * @param repositoryUrl адрес репозитория
 * @param issueNumber номер задачи GitHub
 * @param status статус
 * @param currentStage текущий этап
 * @param completedStages история завершённых этапов
 * @param executionId идентификатор исполнения Temporal, если задан
 */
public record WorkflowState(
		String workflowId,
		String repositoryUrl,
		long issueNumber,
		WorkflowStatus status,
		WorkflowStage currentStage,
		List<WorkflowStage> completedStages,
		String executionId) {
}
