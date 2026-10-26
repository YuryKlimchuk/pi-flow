package com.hydroyura.agents.piflow.workflow.domain;

/**
 * Неизменяемая запись завершённого шага workflow.
 *
 * @param stage этап
 * @param role роль агента
 * @param action действие
 * @param result текст результата
 * @param runId идентификатор запуска
 */
public record CompletedWorkflowStep(String stage, String role, String action, String result, String runId) {
}
