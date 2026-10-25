package com.hydroyura.agents.piflow.github.api;

/**
 * Краткая сводка о задаче (Issue) GitHub.
 *
 * @param number номер задачи
 * @param title заголовок
 * @param state состояние
 * @param author автор
 * @param url URL задачи
 * @param pullRequest является ли элемент pull request
 */
public record IssueSummary(long number, String title, IssueState state, String author, String url, boolean pullRequest) {
}
