package com.hydroyura.agents.piflow.github.api;

/**
 * Фильтр списка задач (Issues) GitHub.
 *
 * @param state состояние задачи или {@code null}, если фильтр по состоянию не применяется
 */
public record IssueFilter(IssueState state) {

	/** Фильтр без ограничений по состоянию. */
	public static final IssueFilter ALL = new IssueFilter(null);

	public static IssueFilter of(IssueState state) {
		return new IssueFilter(state);
	}
}
