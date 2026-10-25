package com.hydroyura.agents.piflow.github.domain;

/**
 * Pull request (внутренняя модель).
 */
public record GithubPullRequest(long number, String url) {
}
