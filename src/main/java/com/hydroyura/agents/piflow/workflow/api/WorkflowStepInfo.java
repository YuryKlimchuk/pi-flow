package com.hydroyura.agents.piflow.workflow.api;

/**
 * Запись завершённого шага workflow для публичного состояния.
 *
 * @param stage этап
 * @param role роль агента
 * @param action действие
 * @param result текст результата
 * @param runId идентификатор запуска
 */
public record WorkflowStepInfo(String stage, String role, String action, String result, String runId) {
}
