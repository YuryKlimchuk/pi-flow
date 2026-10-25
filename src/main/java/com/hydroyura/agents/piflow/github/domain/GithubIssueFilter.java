package com.hydroyura.agents.piflow.github.domain;

/**
 * Фильтр списка задач (внутренняя модель).
 */
public record GithubIssueFilter(GithubIssueState state) {

	public static final GithubIssueFilter ALL = new GithubIssueFilter(null);
}
