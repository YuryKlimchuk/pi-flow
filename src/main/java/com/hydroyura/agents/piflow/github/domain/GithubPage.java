package com.hydroyura.agents.piflow.github.domain;

import java.util.List;

/**
 * Страница данных с метаданными пагинации (внутренняя модель).
 */
public record GithubPage<T>(List<T> items, int page, int perPage, boolean hasNext) {

	public static <T> GithubPage<T> of(List<T> items, int page, int perPage, boolean hasNext) {
		return new GithubPage<>(items, page, perPage, hasNext);
	}
}
