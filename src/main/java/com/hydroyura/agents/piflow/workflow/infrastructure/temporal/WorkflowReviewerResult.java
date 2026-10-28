package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Результат activity независимого ревью Reviewer.
 *
 * @param runId идентификатор запуска агента
 * @param reviewText текст результата ревью
 */
public record WorkflowReviewerResult(String runId, String reviewText) {
}
