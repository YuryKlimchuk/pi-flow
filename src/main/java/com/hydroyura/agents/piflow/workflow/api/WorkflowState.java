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
 * @param analysisText текст артефакта анализа, если анализ завершён
 * @param branchName предложенное имя ветки, если анализ завершён
 * @param designText текст design-артефакта, если проектирование завершено
 * @param implementationRunId идентификатор запуска Developer
 * @param implementationDiagnostics диагностика сборки/тестов реализации
 * @param reviewText текст результата ревью
 * @param reviewRunId идентификатор запуска Reviewer
 * @param errorMessage сообщение об ошибке, если workflow завершился ошибкой
 * @param timeline неизменяемая история завершённых шагов
 * @param commitId идентификатор итогового коммита
 * @param branchUrl URL опубликованной ветки
 * @param prNumber номер созданного pull request (0, если не создан)
 * @param prUrl URL созданного pull request
 */
public record WorkflowState(
		String workflowId,
		String repositoryUrl,
		long issueNumber,
		WorkflowStatus status,
		WorkflowStage currentStage,
		List<WorkflowStage> completedStages,
		String executionId,
		String analysisText,
		String branchName,
		String designText,
		String implementationRunId,
		String implementationDiagnostics,
		String reviewText,
		String reviewRunId,
		String errorMessage,
		List<WorkflowStepInfo> timeline,
		String commitId,
		String branchUrl,
		long prNumber,
		String prUrl) {
}
