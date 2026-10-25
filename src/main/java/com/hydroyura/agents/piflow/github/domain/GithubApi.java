package com.hydroyura.agents.piflow.github.domain;

/**
 * Внутренний порт (driven port) удалённого GitHub API.
 *
 * <p>Реализуется инфраструктурным адаптером; application-слой модуля зависит только
 * от этого порта и преобразует его результаты и ошибки в публичные типы.</p>
 */
public interface GithubApi {

	GithubPage<GithubRepository> listRepositories(int page, int perPage);

	GithubPage<GithubIssue> listIssues(String owner, String repository, GithubIssueFilter filter, int page, int perPage);

	GithubIssueDetails getIssue(String owner, String repository, long number);

	GithubBranchRef publishBranch(String owner, String repository, String branch);

	GithubPullRequest createPullRequest(GithubCreatePullRequest command);
}
