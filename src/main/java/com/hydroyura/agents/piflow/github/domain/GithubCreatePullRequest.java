package com.hydroyura.agents.piflow.github.domain;

/**
 * Параметры создания pull request (внутренняя модель).
 */
public record GithubCreatePullRequest(
		String owner,
		String repository,
		String base,
		String head,
		String title,
		String description) {
}
