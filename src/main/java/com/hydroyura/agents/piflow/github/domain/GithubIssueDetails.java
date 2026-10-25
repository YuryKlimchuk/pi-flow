package com.hydroyura.agents.piflow.github.domain;

/**
 * Полные данные задачи (Issue) GitHub (внутренняя модель).
 */
public record GithubIssueDetails(
		long number,
		String title,
		String description,
		GithubIssueState state,
		String author,
		String url,
		boolean pullRequest) {
}
