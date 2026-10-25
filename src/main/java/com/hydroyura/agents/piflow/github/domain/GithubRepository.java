package com.hydroyura.agents.piflow.github.domain;

/**
 * Репозиторий GitHub (внутренняя модель).
 */
public record GithubRepository(long id, String owner, String name, String url) {
}
