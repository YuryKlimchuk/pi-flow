package com.hydroyura.agents.piflow.github.api;

/**
 * Полные данные задачи (Issue) GitHub.
 */
public record IssueDetails(
		long number,
		String title,
		String description,
		IssueState state,
		String author,
		String url,
		boolean pullRequest) {
}
