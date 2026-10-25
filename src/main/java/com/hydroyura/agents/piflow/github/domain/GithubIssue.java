package com.hydroyura.agents.piflow.github.domain;

/**
 * Задача (Issue) GitHub (внутренняя модель).
 */
public record GithubIssue(
		long number,
		String title,
		GithubIssueState state,
		String author,
		String url,
		boolean pullRequest) {
}
