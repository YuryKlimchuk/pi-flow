package com.hydroyura.agents.piflow.workflow.domain;

/**
 * Контрольная точка процесса.
 *
 * @param name имя контрольной точки
 * @param value зафиксированное значение
 */
public record Checkpoint(String name, String value) {
}
