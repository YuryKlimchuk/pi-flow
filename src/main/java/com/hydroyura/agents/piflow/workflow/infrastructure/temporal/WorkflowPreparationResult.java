package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Результат подготовки workspace в activity.
 *
 * @param path путь к локальной копии репозитория
 * @param branch активная рабочая ветка
 */
public record WorkflowPreparationResult(String path, String branch) {
}
