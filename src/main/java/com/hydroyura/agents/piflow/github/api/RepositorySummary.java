package com.hydroyura.agents.piflow.github.api;

/**
 * Краткая сводка о репозитории GitHub.
 *
 * @param id идентификатор репозитория
 * @param owner владелец репозитория
 * @param name имя репозитория
 * @param url URL репозитория
 */
public record RepositorySummary(long id, String owner, String name, String url) {
}
